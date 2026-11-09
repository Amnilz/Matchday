package il.ac.jmc.matchday.service;

import il.ac.jmc.matchday.dto.EventForm;
import il.ac.jmc.matchday.exception.EntityNotFoundException;
import il.ac.jmc.matchday.dto.MatchForm;
import il.ac.jmc.matchday.dto.MatchResultFilter;
import il.ac.jmc.matchday.dto.MatchSearchCriteria;
import il.ac.jmc.matchday.entity.Match;
import il.ac.jmc.matchday.entity.MatchEvent;
import il.ac.jmc.matchday.entity.MatchEventType;
import il.ac.jmc.matchday.entity.MatchStatus;
import il.ac.jmc.matchday.entity.League;
import il.ac.jmc.matchday.entity.Player;
import il.ac.jmc.matchday.entity.Team;
import il.ac.jmc.matchday.repository.CommentRepository;
import il.ac.jmc.matchday.repository.LeagueRepository;
import il.ac.jmc.matchday.repository.LeagueTeamRepository;
import il.ac.jmc.matchday.repository.MatchEventRepository;
import il.ac.jmc.matchday.repository.MatchRepository;
import il.ac.jmc.matchday.repository.PlayerRepository;
import il.ac.jmc.matchday.repository.TeamRepository;
import il.ac.jmc.matchday.util.Pagination;
import il.ac.jmc.matchday.util.QueryFilters;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing {@link il.ac.jmc.matchday.entity.Match} records.
 * All persistence operations are transactional.
 */
@Service
@Transactional
public class MatchService {

    /** Default sort order for match lists: most recent first. */
    private static final Sort NEWEST_FIRST = Sort.by("matchDate").descending();

    /** Repository for match records. */
    private final MatchRepository      matchRepository;
    /** Repository for match event records. */
    private final MatchEventRepository matchEventRepository;
    /** Repository for comment records. */
    private final CommentRepository    commentRepository;
    /** Repository for player records. */
    private final PlayerRepository     playerRepository;
    /** Repository for team records. */
    private final TeamRepository       teamRepository;
    /** Repository for league records. */
    private final LeagueRepository     leagueRepository;
    /** Repository for league-team memberships. */
    private final LeagueTeamRepository leagueTeamRepository;

    /**
     * Creates the match service with repositories used for match CRUD and validation.
     *
     * @param matchRepository repository for match records
     * @param matchEventRepository repository for recorded match events
     * @param commentRepository repository used when deleting match comments
     * @param playerRepository repository used to validate event players
     * @param teamRepository repository used to resolve and validate teams
     * @param leagueRepository repository used to resolve and validate leagues
     * @param leagueTeamRepository repository used to enforce league membership rules
     */
    public MatchService(MatchRepository matchRepository,
                        MatchEventRepository matchEventRepository,
                        CommentRepository commentRepository,
                        PlayerRepository playerRepository,
                        TeamRepository teamRepository,
                        LeagueRepository leagueRepository,
                        LeagueTeamRepository leagueTeamRepository) {
        this.matchRepository      = matchRepository;
        this.matchEventRepository = matchEventRepository;
        this.commentRepository    = commentRepository;
        this.playerRepository     = playerRepository;
        this.teamRepository       = teamRepository;
        this.leagueRepository     = leagueRepository;
        this.leagueTeamRepository = leagueTeamRepository;
    }

    /**
     * Returns the match with the given id, or throws {@link EntityNotFoundException} (→ HTTP 404)
     * if it does not exist. Use this in controllers instead of duplicating the orElseThrow pattern.
     *
     * @param id match id
     * @return matching match
     * @throws EntityNotFoundException if no match exists with the id
     */
    @Transactional(readOnly = true)
    public Match getByIdOrThrow(Long id) {
        return findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Match.class));
    }

    /**
     * Returns the match with the given primary key, or empty if not found.
     *
     * @param id match id
     * @return matching match, or empty if absent
     */
    public Optional<Match> findById(Long id) {
        return matchRepository.findByIdWithTeams(id);
    }

    /**
     * Returns all matches with the given status, sorted by match date ascending.
     *
     * @param status match status to filter by
     * @return matches with the requested status
     */
    public List<Match> findByStatus(MatchStatus status) {
        return matchRepository.findByStatusOrderByMatchDateAsc(status);
    }

    /**
     * Returns the {@code limit} earliest upcoming (SCHEDULED) matches. DB-level LIMIT; no full scan.
     *
     * @param limit maximum number of matches to return
     * @return upcoming scheduled matches
     */
    public List<Match> findUpcoming(int limit) {
        return matchRepository.findTopByStatusOrderByMatchDateAsc(MatchStatus.SCHEDULED, Pagination.first(limit));
    }

    /**
     * Returns the {@code limit} most recent completed matches, newest first. DB-level LIMIT; no full scan.
     *
     * @param limit maximum number of matches to return
     * @return recent completed matches
     */
    public List<Match> findRecentCompleted(int limit) {
        return matchRepository.findTopByStatusOrderByMatchDateDesc(MatchStatus.COMPLETED, Pagination.first(limit));
    }

    /**
     * Returns matches in a league with the given status, sorted by match date ascending.
     *
     * @param leagueId league id
     * @param status match status to filter by
     * @return matches in the league with the requested status
     */
    public List<Match> findByLeagueIdAndStatus(Long leagueId, MatchStatus status) {
        return matchRepository.findByLeagueIdAndStatusOrderByMatchDateAsc(leagueId, status);
    }

    /**
     * Returns the {@code limit} most recent completed matches in a league, newest first. DB-level LIMIT; no full scan.
     *
     * @param leagueId league id
     * @param limit maximum number of matches to return
     * @return recent completed matches in the league
     */
    public List<Match> findRecentCompletedByLeague(Long leagueId, int limit) {
        return matchRepository.findTopByLeagueIdAndStatusOrderByMatchDateDesc(
                leagueId, MatchStatus.COMPLETED, Pagination.first(limit));
    }

    /**
     * Returns all matches involving the given team with the given status, most recent first.
     *
     * @param teamId team id
     * @param status match status to filter by
     * @return matches involving the team
     */
    public List<Match> findByTeamIdAndStatus(Long teamId, MatchStatus status) {
        return matchRepository.findByTeamIdAndStatus(teamId, status);
    }

    /**
     * Returns a page of matches matching the optional filters, newest first.
     *
     * @param criteria optional search filters
     * @param page zero-based page index
     * @return filtered page of matches
     */
    public Page<Match> findByCriteria(MatchSearchCriteria criteria, int page) {
        if (criteria.hasInvalidDateRange()) return Pagination.empty(page, NEWEST_FIRST);
        return Pagination.page(page, NEWEST_FIRST, pageable -> pageByCriteria(criteria, pageable));
    }

    /**
     * Returns the first {@code limit} matches matching the optional filters plus the total count, newest first.
     *
     * @param criteria optional search filters
     * @param limit maximum number of matches to return
     * @return filtered page limited to the requested size
     */
    public Page<Match> findTopByCriteria(MatchSearchCriteria criteria, int limit) {
        if (criteria.hasInvalidDateRange()) return Page.empty();
        return pageByCriteria(criteria, Pagination.first(limit, NEWEST_FIRST));
    }

    /**
     * Executes a filtered match query with the given pageable.
     *
     * @param criteria active search criteria
     * @param pageable page and sort specification
     * @return one page of matching matches
     */
    private Page<Match> pageByCriteria(MatchSearchCriteria criteria, Pageable pageable) {
        MatchFilterParams params = matchFilterParams(criteria);
        return matchRepository.findByFiltersPageable(
                params.query(),
                params.leagueIds(),
                params.leagueFilterEmpty(),
                params.teamIds(),
                params.teamFilterEmpty(),
                params.statuses(),
                params.statusFilterEmpty(),
                params.resultFilterEmpty(),
                params.homeWin(),
                params.draw(),
                params.awayWin(),
                params.fromDateTime(),
                params.toDateTime(),
                pageable
        );
    }

    /**
     * Converts search criteria into normalised query parameters for the repository.
     *
     * @param criteria active search criteria
     * @return normalised filter parameters
     */
    private MatchFilterParams matchFilterParams(MatchSearchCriteria criteria) {
        String query = (criteria.query() != null && !criteria.query().isBlank())
                ? criteria.query().trim()
                : null;
        LocalDateTime fromDateTime = criteria.fromDate() != null
                ? criteria.fromDate().atStartOfDay()
                : null;
        LocalDateTime toDateTime = criteria.toDate() != null
                ? criteria.toDate().plusDays(1).atStartOfDay()
                : null;
        boolean teamFilterEmpty   = criteria.teamIds()      == null || criteria.teamIds().isEmpty();
        boolean leagueFilterEmpty = criteria.leagueIds()    == null || criteria.leagueIds().isEmpty();
        boolean statusFilterEmpty = criteria.statuses()     == null || criteria.statuses().isEmpty();
        boolean resultFilterEmpty = criteria.resultFilters() == null || criteria.resultFilters().isEmpty();
        List<MatchResultFilter> resultFilters = resultFilterEmpty ? List.of() : criteria.resultFilters();

        return new MatchFilterParams(
                query,
                QueryFilters.idsOrSentinel(criteria.leagueIds()),
                leagueFilterEmpty,
                QueryFilters.idsOrSentinel(criteria.teamIds()),
                teamFilterEmpty,
                statusFilterEmpty ? List.of(MatchStatus.SCHEDULED) : criteria.statuses(),
                statusFilterEmpty,
                resultFilterEmpty,
                resultFilters.contains(MatchResultFilter.HOME_WIN),
                resultFilters.contains(MatchResultFilter.DRAW),
                resultFilters.contains(MatchResultFilter.AWAY_WIN),
                fromDateTime,
                toDateTime
        );
    }

    /**
     * Returns the number of matches with the given status.
     *
     * @param status match status to count
     * @return number of matches with the status
     */
    public long countByStatus(MatchStatus status) {
        return matchRepository.countByStatus(status);
    }

    /**
     * Returns {@code true} if any match (home or away) involves the team with the given ID.
     *
     * @param teamId team id
     * @return true if any match references the team
     */
    public boolean hasMatchesForTeam(Long teamId) {
        return matchRepository.existsByHomeTeamIdOrAwayTeamId(teamId, teamId);
    }

    /**
     * Builds the admin form DTO for an existing match.
     *
     * @param match match entity to copy from
     * @return form populated from the match and its events
     */
    public MatchForm buildForm(Match match) {
        MatchForm form = new MatchForm();
        form.setLeagueId(match.getLeague() != null ? match.getLeague().getId() : null);
        form.setHomeTeamId(match.getHomeTeam().getId());
        form.setAwayTeamId(match.getAwayTeam().getId());
        form.setMatchDate(match.getMatchDate());
        form.setStatus(match.getStatus());
        form.setHomeScore(match.getHomeScore());
        form.setAwayScore(match.getAwayScore());
        List<EventForm> events = new ArrayList<>();
        for (MatchEvent event : matchEventRepository.findByMatchIdOrderByMinuteAsc(match.getId())) {
            EventForm eventForm = new EventForm();
            eventForm.setPlayerId(event.getPlayer().getId());
            eventForm.setType(event.getType());
            eventForm.setMinute(event.getMinute());
            events.add(eventForm);
        }
        form.setEvents(events);
        return form;
    }

    /**
     * Adds admin match errors (details, scores, and event consistency) to the binding result.
     *
     * @param form submitted match form
     * @param result binding result that receives validation errors
     */
    @Transactional(readOnly = true)
    public void validateAdminDetailsForm(MatchForm form, BindingResult result) {
        validateBaseForm(form, result);
        validateCompletedScores(form, result);
        if (form.getStatus() == MatchStatus.COMPLETED && canValidateEventTotals(form, result)) {
            validateSubmittedEvents(form, result);
        }
    }

    /**
     * Validates the base match fields: league, teams, same-team check, league membership, and date rules.
     *
     * @param form form to validate
     * @param result binding result to accumulate errors into
     */
    private void validateBaseForm(MatchForm form, BindingResult result) {
        validateLeagueField(form.getLeagueId(), result);
        validateTeamField(form.getHomeTeamId(), "homeTeamId", "Selected home team does not exist", result);
        validateTeamField(form.getAwayTeamId(), "awayTeamId", "Selected away team does not exist", result);

        if (form.getHomeTeamId() != null && form.getAwayTeamId() != null
                && form.getHomeTeamId().equals(form.getAwayTeamId())) {
            result.rejectValue("awayTeamId", "sameTeam", "Home and away team cannot be the same");
        }

        validateTeamsBelongToLeague(form, result);

        MatchStatus status = form.getStatus() != null ? form.getStatus() : MatchStatus.SCHEDULED;
        if (form.getMatchDate() == null) return;

        LocalDateTime currentMinute = LocalDateTime.now().withSecond(0).withNano(0);
        if (status == MatchStatus.SCHEDULED && form.getMatchDate().isBefore(currentMinute)) {
            result.rejectValue("matchDate", "scheduledInPast", "Scheduled match date cannot be in the past");
        }
        if (status == MatchStatus.COMPLETED && form.getMatchDate().isAfter(LocalDateTime.now())) {
            result.rejectValue("matchDate", "completedInFuture", "Completed match date cannot be in the future");
        }
    }

    /**
     * Rejects missing score fields when the form status is COMPLETED.
     *
     * @param form form to validate
     * @param result binding result to accumulate errors into
     */
    private void validateCompletedScores(MatchForm form, BindingResult result) {
        if (form.getStatus() != MatchStatus.COMPLETED) return;
        if (form.getHomeScore() == null) {
            result.rejectValue("homeScore", "required", "Score is required for a completed match");
        }
        if (form.getAwayScore() == null) {
            result.rejectValue("awayScore", "required", "Score is required for a completed match");
        }
    }

    /**
     * Creates and persists a match from a validated admin form.
     *
     * @param form validated match form
     * @return saved match
     */
    public Match createFromForm(MatchForm form) {
        Match match = new Match(resolveTeam(form.getHomeTeamId()), resolveTeam(form.getAwayTeamId()), form.getMatchDate());
        match.setLeague(resolveLeague(form.getLeagueId()));
        applyAdminForm(match, form);
        Match saved = matchRepository.save(match);
        replaceEventsFromForm(saved, form);
        return saved;
    }

    /**
     * Updates and persists an existing match from a validated admin form.
     *
     * @param match existing match to update
     * @param form validated match form
     * @return saved match
     */
    public Match updateFromForm(Match match, MatchForm form) {
        match.setLeague(resolveLeague(form.getLeagueId()));
        match.setHomeTeam(resolveTeam(form.getHomeTeamId()));
        match.setAwayTeam(resolveTeam(form.getAwayTeamId()));
        match.setMatchDate(form.getMatchDate());
        applyAdminForm(match, form);
        Match saved = matchRepository.save(match);
        replaceEventsFromForm(saved, form);
        return saved;
    }

    /**
     * Deletes the match together with all its comments and events atomically.
     *
     * @param id match id
     */
    public void deleteById(Long id) {
        commentRepository.detachRepliesByMatchId(id);
        commentRepository.deleteByMatchId(id);
        matchEventRepository.deleteByMatchId(id);
        matchRepository.deleteById(id);
    }

    /**
     * Returns true when the event form row is null or all fields are null (empty row to skip).
     *
     * @param event event form row to check
     * @return true when the row is effectively empty
     */
    private boolean isBlankEvent(EventForm event) {
        return event == null
                || (event.getPlayerId() == null
                && event.getType() == null
                && event.getMinute() == null);
    }

    /**
     * Rejects the field when the given team id does not exist in the repository.
     *
     * @param teamId team id to check
     * @param fieldName form field name to reject on failure
     * @param message error message
     * @param result binding result to accumulate errors into
     */
    private void validateTeamField(Long teamId, String fieldName, String message, BindingResult result) {
        if (teamId == null || result.hasFieldErrors(fieldName)) return;
        if (!teamRepository.existsById(teamId)) {
            result.rejectValue(fieldName, "invalid", message);
        }
    }

    /**
     * Rejects the leagueId field when the given league does not exist in the repository.
     *
     * @param leagueId league id to check
     * @param result binding result to accumulate errors into
     */
    private void validateLeagueField(Long leagueId, BindingResult result) {
        if (leagueId == null || result.hasFieldErrors("leagueId")) return;
        if (!leagueRepository.existsById(leagueId)) {
            result.rejectValue("leagueId", "invalid", "Selected league does not exist");
        }
    }

    /**
     * Rejects the home/away team fields when either team is not a member of the selected league.
     *
     * @param form form to validate
     * @param result binding result to accumulate errors into
     */
    private void validateTeamsBelongToLeague(MatchForm form, BindingResult result) {
        if (form.getLeagueId() == null
                || result.hasFieldErrors("leagueId")
                || result.hasFieldErrors("homeTeamId")
                || result.hasFieldErrors("awayTeamId")) {
            return;
        }
        if (!leagueTeamRepository.existsByLeagueIdAndTeamId(form.getLeagueId(), form.getHomeTeamId())) {
            result.rejectValue("homeTeamId", "notInLeague", "Home team is not assigned to the selected league");
        }
        if (!leagueTeamRepository.existsByLeagueIdAndTeamId(form.getLeagueId(), form.getAwayTeamId())) {
            result.rejectValue("awayTeamId", "notInLeague", "Away team is not assigned to the selected league");
        }
    }

    /**
     * Returns true when the prerequisite fields for event validation are present and error-free.
     *
     * @param form form to check
     * @param result current binding result
     * @return true when event totals can be validated
     */
    private boolean canValidateEventTotals(MatchForm form, BindingResult result) {
        return form.getHomeTeamId() != null
                && form.getAwayTeamId() != null
                && form.getHomeScore() != null
                && form.getAwayScore() != null
                && !result.hasFieldErrors("homeTeamId")
                && !result.hasFieldErrors("awayTeamId")
                && !result.hasFieldErrors("homeScore")
                && !result.hasFieldErrors("awayScore");
    }

    /**
     * Validates all submitted event rows: card sequences, goal totals, assist totals, and self-assists.
     *
     * @param form form containing submitted events
     * @param result binding result to accumulate errors into
     */
    private void validateSubmittedEvents(MatchForm form, BindingResult result) {
        List<DraftEvent> drafts = collectDraftEvents(form, result);
        if (hasEventFieldErrors(result)) return;

        List<DraftEvent> effectiveDrafts = applyEffectiveCardTypes(drafts);
        validateCardSequence(effectiveDrafts, result);
        validateGoalTotals(form, effectiveDrafts, result);
        validateAssistTotals(form, effectiveDrafts, result);
        validateAssistMinuteTotals(effectiveDrafts, result);
        validateSelfAssists(effectiveDrafts, result);
    }

    /**
     * Parses and field-validates each event row, returning the valid draft events.
     *
     * @param form form containing submitted events
     * @param result binding result to accumulate field errors into
     * @return list of valid draft events
     */
    private List<DraftEvent> collectDraftEvents(MatchForm form, BindingResult result) {
        List<DraftEvent> drafts = new ArrayList<>();
        List<EventForm> events = form.getEvents() != null ? form.getEvents() : List.of();
        Map<Long, Player> playerCache = new HashMap<>();

        for (int i = 0; i < events.size(); i++) {
            EventForm event = events.get(i);
            if (event == null) continue;

            Player player = null;
            if (event.getPlayerId() == null) {
                result.rejectValue(eventField(i, "playerId"), "required", "Player is required");
            } else {
                player = playerCache.computeIfAbsent(event.getPlayerId(),
                        playerId -> playerRepository.findById(playerId).orElse(null));
                if (player == null) {
                    result.rejectValue(eventField(i, "playerId"), "invalid", "Selected player does not exist");
                } else if (!playerBelongsToMatchTeams(player, form)) {
                    result.rejectValue(eventField(i, "playerId"), "wrongTeam", "Player must belong to one of the selected teams");
                }
            }

            if (event.getType() == null) {
                result.rejectValue(eventField(i, "type"), "required", "Event type is required");
            }
            if (event.getMinute() == null) {
                result.rejectValue(eventField(i, "minute"), "required", "Minute is required");
            } else if (event.getMinute() < EventForm.MINUTE_MIN) {
                result.rejectValue(eventField(i, "minute"), "min",
                        "Minute must be at least " + EventForm.MINUTE_MIN);
            } else if (event.getMinute() > EventForm.MINUTE_MAX) {
                result.rejectValue(eventField(i, "minute"), "max",
                        "Minute must be at most " + EventForm.MINUTE_MAX);
            }

            if (player != null && event.getType() != null && event.getMinute() != null
                    && event.getMinute() >= EventForm.MINUTE_MIN
                    && event.getMinute() <= EventForm.MINUTE_MAX) {
                drafts.add(new DraftEvent(i, player, event.getType(), event.getType(), event.getMinute()));
            }
        }

        return drafts;
    }

    /**
     * Returns true when the player's team is one of the two teams in the form.
     *
     * @param player player to check
     * @param form form holding the home and away team ids
     * @return true when the player belongs to a match team
     */
    private boolean playerBelongsToMatchTeams(Player player, MatchForm form) {
        Long teamId = player.getTeam().getId();
        return teamId.equals(form.getHomeTeamId()) || teamId.equals(form.getAwayTeamId());
    }

    /**
     * Returns true when the binding result contains any field error on an event row.
     *
     * @param result current binding result
     * @return true when any events[] field has an error
     */
    private boolean hasEventFieldErrors(BindingResult result) {
        return result.getFieldErrors().stream()
                .anyMatch(error -> error.getField().startsWith("events["));
    }

    /**
     * Returns a new draft list with second-yellow cards promoted to red cards.
     *
     * @param drafts original draft events
     * @return draft events with effective card types applied
     */
    private List<DraftEvent> applyEffectiveCardTypes(List<DraftEvent> drafts) {
        Map<Integer, MatchEventType> effectiveTypesByIndex = computeEffectiveTypesByIndex(drafts);
        List<DraftEvent> effectiveDrafts = new ArrayList<>();
        for (DraftEvent draft : drafts) {
            effectiveDrafts.add(new DraftEvent(
                    draft.index(),
                    draft.player(),
                    draft.requestedType(),
                    effectiveTypesByIndex.getOrDefault(draft.index(), draft.requestedType()),
                    draft.minute()
            ));
        }
        return effectiveDrafts;
    }

    /**
     * Maps each draft index to its effective type: a player's second yellow card becomes a red card.
     *
     * @param drafts draft events to process
     * @return map from draft index to effective event type
     */
    private Map<Integer, MatchEventType> computeEffectiveTypesByIndex(List<DraftEvent> drafts) {
        List<DraftEvent> sorted = drafts.stream()
                .sorted(Comparator.comparing(DraftEvent::minute).thenComparingInt(DraftEvent::index))
                .toList();
        Map<Long, Integer> yellowCountByPlayer = new HashMap<>();
        Map<Integer, MatchEventType> effectiveTypesByIndex = new HashMap<>();

        for (DraftEvent draft : sorted) {
            MatchEventType effectiveType = draft.requestedType();
            Long playerId = draft.player().getId();
            if (draft.requestedType() == MatchEventType.YELLOW_CARD) {
                int count = yellowCountByPlayer.merge(playerId, 1, Integer::sum);
                if (count == 2) {
                    effectiveType = MatchEventType.RED_CARD;
                }
            }
            effectiveTypesByIndex.put(draft.index(), effectiveType);
        }
        return effectiveTypesByIndex;
    }

    /**
     * Rejects duplicate red cards and events occurring after a red card for the same player.
     *
     * @param drafts draft events with effective types applied
     * @param result binding result to accumulate errors into
     */
    private void validateCardSequence(List<DraftEvent> drafts, BindingResult result) {
        Map<Long, Integer> firstRedMinuteByPlayer = new HashMap<>();
        Map<Long, Integer> redCountByPlayer = new HashMap<>();

        for (DraftEvent draft : drafts) {
            if (draft.effectiveType() != MatchEventType.RED_CARD) continue;
            Long playerId = draft.player().getId();
            int redCount = redCountByPlayer.getOrDefault(playerId, 0) + 1;
            redCountByPlayer.put(playerId, redCount);
            firstRedMinuteByPlayer.merge(playerId, draft.minute(), Math::min);
            if (redCount > 1) {
                result.rejectValue(eventField(draft.index(), "type"), "secondRed",
                        "A player cannot receive more than 1 red card");
            }
        }

        for (DraftEvent draft : drafts) {
            Integer redMinute = firstRedMinuteByPlayer.get(draft.player().getId());
            if (redMinute == null || draft.effectiveType() == MatchEventType.RED_CARD) continue;
            if (draft.minute() > redMinute) {
                result.rejectValue(eventField(draft.index(), "minute"), "afterRed",
                        "A player cannot participate in events after a red card");
            }
        }
    }

    /**
     * Rejects the score fields when the submitted goal event count does not match the recorded score.
     *
     * @param form form containing scores
     * @param drafts draft events with effective types
     * @param result binding result to accumulate errors into
     */
    private void validateGoalTotals(MatchForm form, List<DraftEvent> drafts, BindingResult result) {
        Map<Long, Long> goalsByTeam = countByTeam(drafts, MatchEventType.GOAL);
        long homeGoals = goalsByTeam.getOrDefault(form.getHomeTeamId(), 0L);
        long awayGoals = goalsByTeam.getOrDefault(form.getAwayTeamId(), 0L);

        if (form.getHomeScore().longValue() != homeGoals) {
            result.rejectValue("homeScore", "goalsMismatch",
                    "Home score must match " + homeGoals + " goal events for the home team");
        }
        if (form.getAwayScore().longValue() != awayGoals) {
            result.rejectValue("awayScore", "goalsMismatch",
                    "Away score must match " + awayGoals + " goal events for the away team");
        }
    }

    /**
     * Rejects the score fields when the assist count for a team exceeds the score.
     *
     * @param form form containing scores
     * @param drafts draft events with effective types
     * @param result binding result to accumulate errors into
     */
    private void validateAssistTotals(MatchForm form, List<DraftEvent> drafts, BindingResult result) {
        Map<Long, Long> assistsByTeam = countByTeam(drafts, MatchEventType.ASSIST);
        long homeAssists = assistsByTeam.getOrDefault(form.getHomeTeamId(), 0L);
        long awayAssists = assistsByTeam.getOrDefault(form.getAwayTeamId(), 0L);

        if (homeAssists > form.getHomeScore()) {
            result.rejectValue("homeScore", "assistsExceedScore",
                    "Home assists cannot exceed the recorded home score");
        }
        if (awayAssists > form.getAwayScore()) {
            result.rejectValue("awayScore", "assistsExceedScore",
                    "Away assists cannot exceed the recorded away score");
        }
    }

    /**
     * Rejects assist events whose minute has no matching goal event for the same team.
     *
     * @param drafts draft events with effective types
     * @param result binding result to accumulate errors into
     */
    private void validateAssistMinuteTotals(List<DraftEvent> drafts, BindingResult result) {
        Map<TeamMinute, Long> goalsByTeamMinute = countByTeamMinute(drafts, MatchEventType.GOAL);
        Map<TeamMinute, Long> assistsByTeamMinute = countByTeamMinute(drafts, MatchEventType.ASSIST);

        for (DraftEvent draft : drafts) {
            if (draft.effectiveType() != MatchEventType.ASSIST) continue;
            TeamMinute key = new TeamMinute(draft.player().getTeam().getId(), draft.minute());
            long goalsAtMinute = goalsByTeamMinute.getOrDefault(key, 0L);
            long assistsAtMinute = assistsByTeamMinute.getOrDefault(key, 0L);
            if (goalsAtMinute == 0) {
                result.rejectValue(eventField(draft.index(), "minute"), "assistMinuteMismatch",
                        "Assist minute must match a goal minute for the same team");
            } else if (assistsAtMinute > goalsAtMinute) {
                result.rejectValue(eventField(draft.index(), "type"), "tooManyAssistsAtMinute",
                        "Assist events at this minute cannot exceed goals at the same minute");
            }
        }
    }

    /**
     * Rejects assist events where the player also scored a goal in the same minute.
     *
     * @param drafts draft events with effective types
     * @param result binding result to accumulate errors into
     */
    private void validateSelfAssists(List<DraftEvent> drafts, BindingResult result) {
        Map<PlayerMinute, Boolean> playerGoalsByMinute = new HashMap<>();
        for (DraftEvent draft : drafts) {
            if (draft.effectiveType() == MatchEventType.GOAL) {
                playerGoalsByMinute.put(new PlayerMinute(draft.player().getId(), draft.minute()), true);
            }
        }

        for (DraftEvent draft : drafts) {
            if (draft.effectiveType() != MatchEventType.ASSIST) continue;
            PlayerMinute key = new PlayerMinute(draft.player().getId(), draft.minute());
            if (playerGoalsByMinute.containsKey(key)) {
                result.rejectValue(eventField(draft.index(), "playerId"), "selfAssist",
                        "A player cannot assist their own goal");
            }
        }
    }

    /**
     * Counts draft events of the given type, grouped by the player's team id.
     *
     * @param drafts draft events to count
     * @param type event type filter
     * @return count per team id
     */
    private Map<Long, Long> countByTeam(List<DraftEvent> drafts, MatchEventType type) {
        Map<Long, Long> counts = new HashMap<>();
        for (DraftEvent draft : drafts) {
            if (draft.effectiveType() != type) continue;
            counts.merge(draft.player().getTeam().getId(), 1L, Long::sum);
        }
        return counts;
    }

    /**
     * Counts draft events of the given type, grouped by team id and minute.
     *
     * @param drafts draft events to count
     * @param type event type filter
     * @return count per team-minute composite key
     */
    private Map<TeamMinute, Long> countByTeamMinute(List<DraftEvent> drafts, MatchEventType type) {
        Map<TeamMinute, Long> counts = new HashMap<>();
        for (DraftEvent draft : drafts) {
            if (draft.effectiveType() != type) continue;
            TeamMinute key = new TeamMinute(draft.player().getTeam().getId(), draft.minute());
            counts.merge(key, 1L, Long::sum);
        }
        return counts;
    }

    /**
     * Deletes all events for the match and re-persists them from the submitted form.
     *
     * @param match match whose events to replace
     * @param form form containing the new events
     */
    private void replaceEventsFromForm(Match match, MatchForm form) {
        matchEventRepository.deleteByMatchId(match.getId());
        if (match.getStatus() != MatchStatus.COMPLETED) return;

        for (DraftEvent draft : toEffectiveDraftEvents(form)) {
            matchEventRepository.save(new MatchEvent(match, draft.player(), draft.effectiveType(), draft.minute()));
        }
    }

    /**
     * Converts non-blank event form rows to draft events with effective card types applied.
     *
     * @param form form containing event rows
     * @return effective draft events ready for persistence
     */
    private List<DraftEvent> toEffectiveDraftEvents(MatchForm form) {
        List<DraftEvent> drafts = new ArrayList<>();
        List<EventForm> events = form.getEvents() != null ? form.getEvents() : List.of();
        for (int i = 0; i < events.size(); i++) {
            EventForm event = events.get(i);
            if (isBlankEvent(event)) continue;
            Player player = resolvePlayer(event.getPlayerId());
            drafts.add(new DraftEvent(i, player, event.getType(), event.getType(), event.getMinute()));
        }
        return applyEffectiveCardTypes(drafts);
    }

    /**
     * Returns the Spring binding path for an event row field, e.g. {@code events[0].playerId}.
     *
     * @param index event row index
     * @param fieldName field name within the row
     * @return Spring binding path
     */
    private String eventField(int index, String fieldName) {
        return "events[" + index + "]." + fieldName;
    }

    /**
     * Returns the team with the given id, or throws {@link EntityNotFoundException}.
     *
     * @param teamId team id to look up
     * @return matching team
     */
    private Team resolveTeam(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException(Team.class));
    }

    /**
     * Returns the league with the given id, or throws {@link EntityNotFoundException}.
     *
     * @param leagueId league id to look up
     * @return matching league
     */
    private League resolveLeague(Long leagueId) {
        return leagueRepository.findById(leagueId)
                .orElseThrow(() -> new EntityNotFoundException(League.class));
    }

    /**
     * Returns the player with the given id, or throws {@link EntityNotFoundException}.
     *
     * @param playerId player id to look up
     * @return matching player
     */
    private Player resolvePlayer(Long playerId) {
        return playerRepository.findById(playerId)
                .orElseThrow(() -> new EntityNotFoundException(Player.class));
    }

    /**
     * Applies status and score fields from a validated form onto the match entity.
     *
     * @param match match entity to update
     * @param form validated form with new values
     */
    private void applyAdminForm(Match match, MatchForm form) {
        MatchStatus status = form.getStatus() != null ? form.getStatus() : MatchStatus.SCHEDULED;
        match.setStatus(status);
        if (status == MatchStatus.COMPLETED) {
            match.setHomeScore(form.getHomeScore());
            match.setAwayScore(form.getAwayScore());
        } else {
            match.setHomeScore(null);
            match.setAwayScore(null);
        }
    }

    /**
     * Normalised repository query parameters derived from a {@link il.ac.jmc.matchday.dto.MatchSearchCriteria}.
     *
     * @param query optional text query
     * @param leagueIds league id filter
     * @param leagueFilterEmpty true when the league filter is empty
     * @param teamIds team id filter
     * @param teamFilterEmpty true when the team filter is empty
     * @param statuses status filter
     * @param statusFilterEmpty true when the status filter is empty
     * @param resultFilterEmpty true when the result filter is empty
     * @param homeWin true to include home-win matches
     * @param draw true to include draw matches
     * @param awayWin true to include away-win matches
     * @param fromDateTime optional lower bound on match date
     * @param toDateTime optional upper bound on match date
     */
    private record MatchFilterParams(String query,
                                     List<Long> leagueIds,
                                     boolean leagueFilterEmpty,
                                     List<Long> teamIds,
                                     boolean teamFilterEmpty,
                                     List<MatchStatus> statuses,
                                     boolean statusFilterEmpty,
                                     boolean resultFilterEmpty,
                                     boolean homeWin,
                                     boolean draw,
                                     boolean awayWin,
                                     LocalDateTime fromDateTime,
                                     LocalDateTime toDateTime) {}

    /**
     * Intermediate event representation used during form validation and persistence.
     *
     * @param index position of this row in the events list
     * @param player resolved player entity
     * @param requestedType event type as submitted by the form
     * @param effectiveType effective type after second-yellow promotion
     * @param minute minute the event occurred
     */
    private record DraftEvent(int index,
                              Player player,
                              MatchEventType requestedType,
                              MatchEventType effectiveType,
                              Integer minute) {}

    /**
     * Composite key used to group events by team and minute during validation.
     *
     * @param teamId team id
     * @param minute event minute
     */
    private record TeamMinute(Long teamId, Integer minute) {}

    /**
     * Composite key used to detect self-assists during validation.
     *
     * @param playerId player id
     * @param minute event minute
     */
    private record PlayerMinute(Long playerId, Integer minute) {}
}
