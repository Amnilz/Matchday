package il.ac.jmc.matchday.service;

import il.ac.jmc.matchday.dto.PlayerStats;
import il.ac.jmc.matchday.entity.MatchEvent;
import il.ac.jmc.matchday.entity.MatchEventType;
import il.ac.jmc.matchday.repository.MatchEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for reading {@link MatchEvent} records and computing per-player stats.
 * Admin match/event writes are handled atomically by {@link MatchService}.
 */
@Service
@Transactional(readOnly = true)
public class MatchEventService {

    /** Repository for event reads and aggregate queries. */
    private final MatchEventRepository matchEventRepository;

    /**
     * Creates the match-event service.
     *
     * @param matchEventRepository repository for event reads and aggregate queries
     */
    public MatchEventService(MatchEventRepository matchEventRepository) {
        this.matchEventRepository = matchEventRepository;
    }

    /**
     * Returns all events for the given match, ordered by minute ascending.
     *
     * @param matchId match id
     * @return ordered match events
     */
    public List<MatchEvent> findByMatchId(Long matchId) {
        return matchEventRepository.findByMatchIdOrderByMinuteAsc(matchId);
    }

    /**
     * Returns all events for the given player, most recent match first.
     *
     * @param playerId player id
     * @return events involving the player
     */
    public List<MatchEvent> findByPlayerId(Long playerId) {
        return matchEventRepository.findByPlayerIdOrderByMatchMatchDateDesc(playerId);
    }

    /**
     * Returns true when the player is referenced by recorded match events.
     *
     * @param playerId player id
     * @return true if recorded events reference the player
     */
    public boolean hasEventsForPlayer(Long playerId) {
        return matchEventRepository.existsByPlayerId(playerId);
    }

    /**
     * Returns stats for a batch of players in a single query.
     * Use this on list pages to avoid N+1 individual stat lookups.
     *
     * @param playerIds player ids to calculate stats for
     * @return mapping from player id to stats
     */
    public Map<Long, PlayerStats> getStatsForPlayers(List<Long> playerIds) {
        if (playerIds.isEmpty()) return Map.of();

        Map<Long, int[]> counts = new HashMap<>();
        for (Object[] row : matchEventRepository.countByPlayerIdsGroupedByType(playerIds)) {
            Long playerId       = (Long) row[0];
            MatchEventType type = (MatchEventType) row[1];
            int count           = ((Long) row[2]).intValue();
            int[] c = counts.computeIfAbsent(playerId, _ -> new int[4]);
            switch (type) {
                case GOAL        -> c[0] += count;
                case ASSIST      -> c[1] += count;
                case YELLOW_CARD -> c[2] += count;
                case RED_CARD    -> c[3] += count;
            }
        }

        Map<Long, PlayerStats> result = new HashMap<>();
        for (Long id : playerIds) {
            int[] c = counts.getOrDefault(id, new int[4]);
            result.put(id, new PlayerStats(c[0], c[1], c[2], c[3]));
        }
        return result;
    }

    /**
     * Groups an already-loaded list of events by their match ID.
     * Use this when events have already been fetched to avoid redundant round-trips.
     *
     * @param events events to group
     * @return events grouped by match id
     */
    public Map<Long, List<MatchEvent>> groupByMatchId(List<MatchEvent> events) {
        return events.stream()
                .collect(Collectors.groupingBy(e -> e.getMatch().getId()));
    }

    /**
     * Returns an ordered map of playerId → count for the top {@code limit} players
     * of the given event type. One DB query; ordered by count descending.
     *
     * @param type event type to rank
     * @param limit maximum number of players to return
     * @return ordered player id to event count mapping
     */
    public Map<Long, Long> getTopPlayerIds(MatchEventType type, int limit) {
        Map<Long, Long> result = new LinkedHashMap<>();
        for (Object[] row : matchEventRepository.findTopPlayerIdsByType(type, PageRequest.of(0, limit))) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }

    /**
     * Computes stats from an already-loaded list of events - no extra DB queries.
     * Use this when events have already been fetched to avoid redundant round-trips.
     *
     * @param events events to summarize
     * @return computed player stats
     */
    public PlayerStats computeStats(List<MatchEvent> events) {
        int goals = 0, assists = 0, yellows = 0, reds = 0;
        for (MatchEvent e : events) {
            switch (e.getType()) {
                case GOAL        -> goals++;
                case ASSIST      -> assists++;
                case YELLOW_CARD -> yellows++;
                case RED_CARD    -> reds++;
            }
        }
        return new PlayerStats(goals, assists, yellows, reds);
    }
}
