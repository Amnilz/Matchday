package il.ac.jmc.matchday.service;

import il.ac.jmc.matchday.dto.PlayerForm;
import il.ac.jmc.matchday.dto.PlayerListItem;
import il.ac.jmc.matchday.dto.PlayerStats;
import il.ac.jmc.matchday.entity.MatchEventType;
import il.ac.jmc.matchday.entity.Player;
import il.ac.jmc.matchday.entity.Position;
import il.ac.jmc.matchday.entity.Team;
import il.ac.jmc.matchday.exception.EntityNotFoundException;
import il.ac.jmc.matchday.repository.PlayerRepository;
import il.ac.jmc.matchday.util.Pagination;
import il.ac.jmc.matchday.util.QueryFilters;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for managing {@link il.ac.jmc.matchday.entity.Player} records.
 * All persistence operations are transactional.
 */
@Service
@Transactional
public class PlayerService {

    /** Repository for player records. */
    private final PlayerRepository playerRepository;
    /** Service for match-event statistics. */
    private final MatchEventService matchEventService;

    /**
     * Creates the service with player storage and event statistics collaborators.
     *
     * @param playerRepository player repository
     * @param matchEventService match-event service
     */
    public PlayerService(PlayerRepository playerRepository,
                         MatchEventService matchEventService) {
        this.playerRepository = playerRepository;
        this.matchEventService = matchEventService;
    }

    /**
     * Returns all players across all teams.
     *
     * @return all players
     */
    public List<Player> findAll() {
        return playerRepository.findAll();
    }

    /**
     * Returns the player with the given primary key, or empty if not found.
     *
     * @param id player id
     * @return matching player, if present
     */
    public Optional<Player> findById(Long id) {
        return playerRepository.findById(id);
    }

    /**
     * Returns the player with the given id, or throws {@link EntityNotFoundException} (→ HTTP 404)
     * if it does not exist. Use this in controllers instead of duplicating the orElseThrow pattern.
     *
     * @param id player id
     * @return matching player
     */
    @Transactional(readOnly = true)
    public Player getByIdOrThrow(Long id) {
        return playerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Player.class));
    }

    /**
     * Builds a pre-populated form DTO from an existing player entity.
     *
     * @param player source player
     * @return populated form
     */
    public PlayerForm buildForm(Player player) {
        PlayerForm form = new PlayerForm();
        form.setFullName(player.getFullName());
        form.setTeamId(player.getTeam().getId());
        form.setPosition(player.getPosition());
        form.setShirtNumber(player.getShirtNumber());
        form.setPhotoUrl(player.getPhotoUrl());
        form.setActive(player.isActive());
        return form;
    }

    /**
     * Returns active players in the given team, ordered by shirt number. Used by public-facing pages.
     *
     * @param teamId team id
     * @return active players in the team
     */
    public List<Player> findActiveByTeamId(Long teamId) {
        return playerRepository.findByTeamIdAndActiveTrueOrderByShirtNumberAsc(teamId);
    }

    /**
     * Returns {@code true} if the given shirt number is already taken within the given team.
     *
     * @param team team to check
     * @param shirtNumber shirt number to check
     * @return true when taken
     */
    public boolean shirtNumberTaken(Team team, Integer shirtNumber) {
        return playerRepository.existsByTeamAndShirtNumber(team, shirtNumber);
    }

    /**
     * Returns {@code true} if the team identified by {@code teamId} has at least one player.
     *
     * @param teamId team id
     * @return true when the team has players
     */
    public boolean hasPlayersInTeam(Long teamId) {
        return playerRepository.existsByTeamId(teamId);
    }

    /**
     * Returns the first {@code limit} active players matching any combination of name, teams, and positions.
     *
     * @param name optional name filter
     * @param teamIds optional team filters
     * @param positions optional position filters
     * @param limit maximum rows to return
     * @return first page of active players
     */
    public Page<Player> findActiveByFilters(String name, List<Long> teamIds, List<Position> positions, int limit) {
        PlayerFilterParams params = playerFilterParams(name, teamIds, positions);
        return playerRepository.findActiveByFilters(
                params.name(),
                params.teamIds(),
                params.teamFilterEmpty(),
                params.positions(),
                params.positionFilterEmpty(),
                Pagination.first(limit)
        );
    }

    /**
     * Filters all players (including inactive) by name, team, and position, returning one page.
     * Used by the admin list. Results are ordered by team name then shirt number.
     *
     * @param name optional name filter
     * @param teamIds optional team filters
     * @param positions optional position filters
     * @param page zero-based page number
     * @return page of players
     */
    public Page<Player> listAllFiltered(String name, List<Long> teamIds,
                                         List<Position> positions, int page) {
        PlayerFilterParams params = playerFilterParams(name, teamIds, positions);
        return Pagination.page(page, pageable -> playerRepository.findByFiltersPageable(
                params.name(),
                params.teamIds(),
                params.teamFilterEmpty(),
                params.positions(),
                params.positionFilterEmpty(),
                pageable));
    }

    /**
     * Filters active players with DB-level pagination and stat-aware ordering.
     * Stats are batch-loaded for the page after the ordered player IDs are selected.
     * Sorting keys: "goals", "assists", "yellows", "reds"; direction: "asc" or "desc".
     *
     * @param name optional name filter
     * @param teamIds optional team filters
     * @param positions optional position filters
     * @param sortBy optional statistic sort key
     * @param dir sort direction
     * @param page zero-based page number
     * @return page of player list items
     */
    public Page<PlayerListItem> listFiltered(String name, List<Long> teamIds,
                                              List<Position> positions,
                                              String sortBy, String dir, int page) {
        PlayerFilterParams params = playerFilterParams(name, teamIds, positions);
        Page<Long> playerIds = Pagination.page(page, pageable -> playerRepository.findActiveIdsByFiltersPageable(
                params.name(),
                params.teamIds(),
                params.teamFilterEmpty(),
                params.positions(),
                params.positionFilterEmpty(),
                normalizeSort(sortBy),
                normalizeDirection(dir),
                MatchEventType.GOAL,
                MatchEventType.ASSIST,
                MatchEventType.YELLOW_CARD,
                MatchEventType.RED_CARD,
                pageable));
        return toPlayerListItemPage(playerIds);
    }

    /**
     * Maps a page of player ids to a page of {@link il.ac.jmc.matchday.dto.PlayerListItem} enriched with stats.
     *
     * @param playerIds page of player ids from the repository
     * @return page of player list items with team and stats
     */
    private Page<PlayerListItem> toPlayerListItemPage(Page<Long> playerIds) {
        List<Long> ids = playerIds.getContent();
        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), playerIds.getPageable(), playerIds.getTotalElements());
        }

        Map<Long, Player> playersById = playerRepository.findAllByIdWithTeam(ids)
                .stream().collect(Collectors.toMap(Player::getId, Function.identity()));
        Map<Long, PlayerStats> statsById = matchEventService.getStatsForPlayers(ids);
        List<PlayerListItem> items = new ArrayList<>();
        for (Long id : ids) {
            Player player = playersById.get(id);
            if (player != null) {
                items.add(new PlayerListItem(
                        player,
                        statsById.getOrDefault(id, new PlayerStats(0, 0, 0, 0))));
            }
        }
        return new PageImpl<>(items, playerIds.getPageable(), playerIds.getTotalElements());
    }

    /**
     * Converts raw filter inputs to normalised repository query parameters.
     *
     * @param name optional name filter
     * @param teamIds optional team id filter
     * @param positions optional position filter
     * @return normalised filter parameters
     */
    private PlayerFilterParams playerFilterParams(String name, List<Long> teamIds, List<Position> positions) {
        String nameTrimmed = (name != null && !name.isBlank()) ? name.trim() : null;
        boolean teamFilterEmpty     = teamIds   == null || teamIds.isEmpty();
        boolean positionFilterEmpty = positions == null || positions.isEmpty();
        return new PlayerFilterParams(
                nameTrimmed,
                QueryFilters.idsOrSentinel(teamIds),
                teamFilterEmpty,
                positionFilterEmpty ? List.of(Position.GK) : positions,
                positionFilterEmpty
        );
    }

    /**
     * Returns the sort column when it is a recognised stat name, or empty string for the default order.
     *
     * @param sortBy raw sort parameter
     * @return normalised sort column name
     */
    private String normalizeSort(String sortBy) {
        return switch (sortBy != null ? sortBy : "") {
            case "goals", "assists", "yellows", "reds" -> sortBy;
            default -> "";
        };
    }

    /**
     * Returns {@code "asc"} when the direction is explicitly "asc", otherwise {@code "desc"}.
     *
     * @param dir raw direction parameter
     * @return normalised sort direction
     */
    private String normalizeDirection(String dir) {
        return "asc".equals(dir) ? "asc" : "desc";
    }

    /**
     * Returns the top {@code limit} players ranked by the given event type, ordered by count descending.
     * Uses two targeted DB queries (top IDs + fetch players with team) — no N+1 and no full table scan.
     *
     * @param type event type to rank by
     * @param limit maximum rows to return
     * @return ranked player list items
     */
    public List<PlayerListItem> getTopByType(MatchEventType type, int limit) {
        Map<Long, Long> idCounts = matchEventService.getTopPlayerIds(type, limit);
        if (idCounts.isEmpty()) return List.of();
        Map<Long, Player> playerMap = playerRepository.findAllByIdWithTeam(new ArrayList<>(idCounts.keySet()))
                .stream().collect(Collectors.toMap(Player::getId, Function.identity()));
        return idCounts.entrySet().stream()
                .filter(e -> playerMap.containsKey(e.getKey()))
                .map(e -> {
                    Player p = playerMap.get(e.getKey());
                    int count = e.getValue().intValue();
                    PlayerStats stats = switch (type) {
                        case GOAL        -> new PlayerStats(count, 0, 0, 0);
                        case ASSIST      -> new PlayerStats(0, count, 0, 0);
                        case YELLOW_CARD -> new PlayerStats(0, 0, count, 0);
                        case RED_CARD    -> new PlayerStats(0, 0, 0, count);
                    };
                    return new PlayerListItem(p, stats);
                })
                .toList();
    }

    /**
     * Creates and persists a player from a validated form.
     *
     * @param form validated player form
     * @param team selected team
     * @return saved player
     */
    public Player createFromForm(PlayerForm form, Team team) {
        Player player = new Player(form.getFullName().trim(), form.getPosition(), team);
        player.setShirtNumber(form.getShirtNumber());
        player.setPhotoUrl(form.getPhotoUrl());
        player.setActive(form.isActive());
        return save(player);
    }

    /**
     * Updates and persists an existing player from a validated form.
     *
     * @param player existing player
     * @param form validated player form
     * @param team selected team
     */
    public void updateFromForm(Player player, PlayerForm form, Team team) {
        player.setFullName(form.getFullName().trim());
        player.setTeam(team);
        player.setPosition(form.getPosition());
        player.setShirtNumber(form.getShirtNumber());
        player.setPhotoUrl(form.getPhotoUrl());
        player.setActive(form.isActive());
        save(player);
    }

    /**
     * Validates that the shirt number in the form is not already taken within the team.
     * Skips the check when the number belongs to the player currently being edited.
     *
     * @param form submitted player form
     * @param team selected team
     * @param existing existing player when editing, or {@code null} when creating
     * @param result validation result to receive business-rule errors
     */
    public void validateShirtNumber(PlayerForm form, Team team, Player existing, BindingResult result) {
        if (result.hasFieldErrors("shirtNumber") || form.getShirtNumber() == null || team == null) return;
        boolean taken = shirtNumberTaken(team, form.getShirtNumber());
        boolean samePlayer = existing != null
                && team.getId().equals(existing.getTeam().getId())
                && form.getShirtNumber().equals(existing.getShirtNumber());
        if (taken && !samePlayer) {
            result.rejectValue("shirtNumber", "duplicate",
                    "Shirt number " + form.getShirtNumber() + " is already taken in this team");
        }
    }

    /**
     * Persists or updates the given player and returns the managed instance.
     *
     * @param player player to save
     * @return saved player
     */
    public Player save(Player player) {
        return playerRepository.save(player);
    }

    /**
     * Returns the total number of players.
     *
     * @return player count
     */
    public long count() {
        return playerRepository.count();
    }

    /**
     * Returns the number of players missing a photo URL.
     *
     * @return count of players without photo URLs
     */
    public long countMissingPhotos() {
        return playerRepository.countMissingPhotoUrls();
    }

    /**
     * Deletes the player identified by {@code id}; no-op if not found.
     *
     * @param id player id to delete
     */
    public void deleteById(Long id) {
        if (matchEventService.hasEventsForPlayer(id)) {
            String name = playerRepository.findById(id).map(Player::getFullName).orElse("this player");
            throw new IllegalStateException(
                    "Cannot delete \"" + name + "\" - the player has recorded match events. Mark the player inactive instead.");
        }
        playerRepository.deleteById(id);
    }

    /**
     * Normalised repository query parameters derived from player list filter inputs.
     *
     * @param name optional trimmed name filter
     * @param teamIds team id filter (sentinel when empty)
     * @param teamFilterEmpty true when no team filter was supplied
     * @param positions position filter
     * @param positionFilterEmpty true when no position filter was supplied
     */
    private record PlayerFilterParams(String name,
                                      List<Long> teamIds,
                                      boolean teamFilterEmpty,
                                      List<Position> positions,
                                      boolean positionFilterEmpty) {}
}
