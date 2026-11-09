package il.ac.jmc.matchday.service;

import il.ac.jmc.matchday.dto.LeagueForm;
import il.ac.jmc.matchday.entity.League;
import il.ac.jmc.matchday.entity.LeagueTeam;
import il.ac.jmc.matchday.entity.Team;
import il.ac.jmc.matchday.exception.EntityNotFoundException;
import il.ac.jmc.matchday.repository.LeagueRepository;
import il.ac.jmc.matchday.repository.LeagueTeamRepository;
import il.ac.jmc.matchday.repository.MatchRepository;
import il.ac.jmc.matchday.repository.TeamRepository;
import il.ac.jmc.matchday.util.NameFormatter;
import il.ac.jmc.matchday.util.Pagination;
import il.ac.jmc.matchday.util.QueryFilters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Service for league CRUD and team membership rules.
 */
@Service
@Transactional
public class LeagueService {

    /** Repository for league records. */
    private final LeagueRepository leagueRepository;
    /** Repository for league-team memberships. */
    private final LeagueTeamRepository leagueTeamRepository;
    /** Repository for team records. */
    private final TeamRepository teamRepository;
    /** Repository used to guard deletions referenced by matches. */
    private final MatchRepository matchRepository;

    /**
     * Creates the league service with the repositories needed for league membership rules.
     *
     * @param leagueRepository repository for league records
     * @param leagueTeamRepository repository for league-team memberships
     * @param teamRepository repository for team records
     * @param matchRepository repository used to protect leagues and memberships referenced by matches
     */
    public LeagueService(LeagueRepository leagueRepository,
                         LeagueTeamRepository leagueTeamRepository,
                         TeamRepository teamRepository,
                         MatchRepository matchRepository) {
        this.leagueRepository = leagueRepository;
        this.leagueTeamRepository = leagueTeamRepository;
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
    }

    /**
     * Returns all leagues sorted alphabetically.
     *
     * @return leagues ordered by name
     */
    @Transactional(readOnly = true)
    public List<League> findAll() {
        return leagueRepository.findAllByOrderByNameAsc();
    }

    /**
     * Returns the league with the given id, if it exists.
     *
     * @param id league id
     * @return matching league, or empty if absent
     */
    @Transactional(readOnly = true)
    public Optional<League> findById(Long id) {
        return leagueRepository.findById(id);
    }

    /**
     * Returns the first league alphabetically; used as a default selection (e.g. new match form, table page).
     *
     * @return default league, or empty when there are no leagues
     */
    @Transactional(readOnly = true)
    public Optional<League> findPrimaryLeague() {
        return leagueRepository.findTopByOrderByNameAsc();
    }

    /**
     * Returns a random league on every call; used for the home-page top teams' widget.
     *
     * @return random league, or empty when there are no leagues
     */
    @Transactional(readOnly = true)
    public Optional<League> findRandomLeague() {
        long count = leagueRepository.count();
        if (count == 0) return Optional.empty();
        int offset = ThreadLocalRandom.current().nextInt((int) count);
        return leagueRepository.findPage(PageRequest.of(offset, 1)).stream().findFirst();
    }

    /**
     * Returns the total number of leagues, shown on the admin dashboard.
     *
     * @return league count
     */
    @Transactional(readOnly = true)
    public long count() {
        return leagueRepository.count();
    }

    /**
     * Counts leagues without a logo, shown next to the admin Wikipedia-fetch button.
     *
     * @return number of leagues with no logo URL
     */
    @Transactional(readOnly = true)
    public long countMissingLogos() {
        return leagueRepository.countMissingLogoUrls();
    }

    /**
     * Returns the league's member teams sorted by name.
     *
     * @param leagueId league id
     * @return teams assigned to the league
     */
    @Transactional(readOnly = true)
    public List<Team> findTeams(Long leagueId) {
        return leagueTeamRepository.findByLeagueIdWithTeamOrderByTeamName(leagueId).stream()
                .map(LeagueTeam::getTeam)
                .toList();
    }

    /**
     * Returns the leagues a team belongs to, sorted by league name.
     *
     * @param teamId team id
     * @return leagues containing the team
     */
    @Transactional(readOnly = true)
    public List<League> findLeaguesForTeam(Long teamId) {
        return leagueTeamRepository.findByTeamIdWithLeagueOrderByLeagueName(teamId).stream()
                .map(LeagueTeam::getLeague)
                .toList();
    }

    /**
     * Returns the teams not yet in the league, for the admin "add team" dropdown.
     *
     * @param leagueId league id
     * @return teams that can still be added to the league
     */
    @Transactional(readOnly = true)
    public List<Team> findAvailableTeams(Long leagueId) {
        Set<Long> assignedIds = findTeamIds(leagueId);
        return teamRepository.findAllByOrderByNameAsc().stream()
                .filter(team -> !assignedIds.contains(team.getId()))
                .toList();
    }

    /**
     * Returns the ids of all teams in the league.
     *
     * @param leagueId league id
     * @return ids of teams assigned to the league
     */
    @Transactional(readOnly = true)
    public Set<Long> findTeamIds(Long leagueId) {
        return leagueTeamRepository.findTeamIdsByLeagueId(leagueId);
    }

    /**
     * Maps each given team id to the ids of leagues it belongs to (empty set when none).
     *
     * @param teamIds team ids to inspect
     * @return mapping from team id to league ids
     */
    @Transactional(readOnly = true)
    public Map<Long, Set<Long>> findLeagueIdsByTeamIds(List<Long> teamIds) {
        Map<Long, Set<Long>> result = new HashMap<>();
        if (teamIds == null || teamIds.isEmpty()) return result;
        for (Long teamId : teamIds) {
            result.put(teamId, new LinkedHashSet<>());
        }
        for (Object[] row : leagueTeamRepository.findLeagueIdsByTeamIds(teamIds)) {
            Long teamId = (Long) row[0];
            Long leagueId = (Long) row[1];
            result.computeIfAbsent(teamId, _ -> new LinkedHashSet<>()).add(leagueId);
        }
        return result;
    }

    /**
     * Returns the union of team ids across the given leagues.
     *
     * @param leagueIds league ids to inspect
     * @return team ids that belong to at least one selected league
     */
    @Transactional(readOnly = true)
    public Set<Long> findTeamIdsForLeagues(List<Long> leagueIds) {
        Set<Long> ids = new LinkedHashSet<>();
        if (leagueIds == null) return ids;
        for (Long leagueId : leagueIds) {
            ids.addAll(findTeamIds(leagueId));
        }
        return ids;
    }

    /**
     * Returns a map of leagueId → teamCount for the given league IDs in a single query.
     * Leagues with no teams default to 0.
     *
     * @param leagueIds league ids to count teams for
     * @return mapping from league id to team count
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> findTeamCounts(List<Long> leagueIds) {
        Map<Long, Long> counts = new HashMap<>();
        leagueIds.forEach(id -> counts.put(id, 0L));
        if (!leagueIds.isEmpty()) {
            for (Object[] row : leagueTeamRepository.countTeamsByLeagueIds(leagueIds)) {
                counts.put((Long) row[0], (Long) row[1]);
            }
        }
        return counts;
    }

    /**
     * Intersects selected teams with selected leagues. If a league filter matches no teams,
     * returns a sentinel team id so repository "empty means no filter" behavior does not widen results.
     *
     * @param leagueIds selected league ids
     * @param teamIds selected team ids, or empty for no team filter
     * @return effective team ids to pass to repository filters
     */
    @Transactional(readOnly = true)
    public List<Long> effectiveTeamIdsForLeagueFilter(List<Long> leagueIds, List<Long> teamIds) {
        if (leagueIds == null || leagueIds.isEmpty()) return teamIds;

        Set<Long> leagueTeamIds = findTeamIdsForLeagues(leagueIds);

        if (teamIds == null || teamIds.isEmpty()) {
            return leagueTeamIds.isEmpty() ? List.of(QueryFilters.NO_MATCH_ID) : new ArrayList<>(leagueTeamIds);
        }

        List<Long> matchingTeamIds = teamIds.stream()
                .filter(leagueTeamIds::contains)
                .toList();
        return matchingTeamIds.isEmpty() ? List.of(QueryFilters.NO_MATCH_ID) : matchingTeamIds;
    }

    /**
     * Returns a map of teamId → comma-joined league IDs for the given teams,
     * used by the admin match form to filter team dropdowns by league client-side.
     *
     * @param teams teams to map
     * @return mapping from team id to comma-separated league ids
     */
    @Transactional(readOnly = true)
    public Map<Long, String> findLeagueIdsCsvByTeam(List<Team> teams) {
        List<Long> teamIds = teams.stream().map(Team::getId).toList();
        Map<Long, Set<Long>> leagueIdsByTeam = findLeagueIdsByTeamIds(teamIds);
        Map<Long, String> result = new HashMap<>();
        for (Team team : teams) {
            String csv = leagueIdsByTeam.getOrDefault(team.getId(), Set.of()).stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            result.put(team.getId(), csv);
        }
        return result;
    }

    /**
     * Validates uniqueness of the league name and adds a binding error when violated.
     * Call after standard {@code @Valid} validation and only when there are no existing name field errors.
     *
     * @param form submitted league form
     * @param result binding result that receives duplicate-name errors
     * @param editId the ID of the league being edited, or {@code null} for a new league
     */
    public void validateForm(LeagueForm form, BindingResult result, Long editId) {
        if (result.hasFieldErrors("name") || form.getName() == null) return;
        String trimmed = form.getName().trim();
        boolean duplicate = editId != null
                ? leagueRepository.existsByNameIgnoreCaseAndIdNot(trimmed, editId)
                : leagueRepository.existsByNameIgnoreCase(trimmed);
        if (duplicate) {
            result.rejectValue("name", "duplicate", "A league with this name already exists");
        }
    }

    /**
     * Builds a pre-populated form DTO from an existing league entity.
     *
     * @param league league entity to copy from
     * @return form populated from the league
     */
    public LeagueForm buildForm(League league) {
        LeagueForm form = new LeagueForm();
        form.setName(league.getName());
        form.setDescription(league.getDescription());
        form.setLogoUrl(league.getLogoUrl());
        form.setActive(league.isActive());
        return form;
    }

    /**
     * Creates a new league from the validated admin form.
     *
     * @param form validated league form
     * @return saved league
     */
    public League createFromForm(LeagueForm form) {
        League league = new League(form.getName().trim());
        applyForm(league, form);
        return save(league);
    }

    /**
     * Applies the validated admin form to an existing league and saves it.
     *
     * @param league existing league to update
     * @param form validated league form
     */
    public void updateFromForm(League league, LeagueForm form) {
        league.setName(form.getName() != null ? form.getName().trim() : "");
        applyForm(league, form);
        save(league);
    }

    /**
     * Saves a league, normalizing its name to title case first.
     *
     * @param league league to save
     * @return saved league
     */
    public League save(League league) {
        if (league.getName() != null) {
            league.setName(NameFormatter.toTitleCase(league.getName()));
        }
        return leagueRepository.save(league);
    }

    /**
     * Adds a team to a league; already-member teams are a no-op.
     *
     * @param leagueId league id
     * @param teamId team id
     */
    @Transactional
    public void addTeam(Long leagueId, Long teamId) {
        League league = resolveLeague(leagueId);
        Team team = resolveTeam(teamId);
        if (leagueTeamRepository.existsByLeagueIdAndTeamId(leagueId, teamId)) return;
        leagueTeamRepository.save(new LeagueTeam(league, team));
    }

    /**
     * Removes a team from a league.
     *
     * @param leagueId league id
     * @param teamId team id
     * @throws IllegalStateException if the team has matches in the league
     */
    @Transactional
    public void removeTeam(Long leagueId, Long teamId) {
        if (matchRepository.existsByLeagueIdAndTeamId(leagueId, teamId)) {
            throw new IllegalStateException(
                    "Cannot remove this team from the league - it has matches in this league.");
        }
        leagueTeamRepository.deleteByLeagueIdAndTeamId(leagueId, teamId);
    }

    /**
     * Deletes a league and all its team memberships atomically.
     *
     * @param id league id
     * @throws IllegalStateException if any match still references this league
     */
    @Transactional
    public void deleteById(Long id) {
        League league = resolveLeague(id);
        if (matchRepository.existsByLeagueId(id)) {
            throw new IllegalStateException(
                    "Cannot delete \"" + league.getName() + "\" - it still has matches.");
        }
        leagueTeamRepository.deleteByLeagueId(id);
        leagueRepository.deleteById(id);
    }

    /**
     * Copies editable fields from the form onto the league entity.
     *
     * @param league league entity to update
     * @param form form with new values
     */
    private void applyForm(League league, LeagueForm form) {
        league.setDescription(form.getDescription());
        league.setLogoUrl(form.getLogoUrl());
        league.setActive(form.isActive());
    }

    /**
     * Returns the league with the given id, or throws {@link EntityNotFoundException} (→ HTTP 404)
     * if it does not exist. Use this in controllers instead of duplicating the orElseThrow pattern.
     *
     * @param id league id
     * @return matching league
     * @throws EntityNotFoundException if no league exists with the id
     */
    @Transactional(readOnly = true)
    public League getByIdOrThrow(Long id) {
        return resolveLeague(id);
    }

    /**
     * Returns one page of leagues filtered by optional name/description text, sorted alphabetically.
     *
     * @param name optional text filter
     * @param page zero-based page index
     * @return filtered page of leagues
     */
    @Transactional(readOnly = true)
    public Page<League> findBySearchPage(String name, int page) {
        boolean nameFilterEmpty = name == null || name.isBlank();
        return Pagination.page(page, pageable -> leagueRepository.findBySearchPageable(
                nameFilterEmpty ? "" : name.trim(),
                nameFilterEmpty,
                pageable));
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
     * Returns the team with the given id, or throws {@link EntityNotFoundException}.
     *
     * @param teamId team id to look up
     * @return matching team
     */
    private Team resolveTeam(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException(Team.class));
    }
}
