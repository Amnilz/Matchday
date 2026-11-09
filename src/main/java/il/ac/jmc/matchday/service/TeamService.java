package il.ac.jmc.matchday.service;

import il.ac.jmc.matchday.dto.TeamForm;
import il.ac.jmc.matchday.dto.TeamSummary;
import il.ac.jmc.matchday.entity.Team;
import il.ac.jmc.matchday.exception.EntityNotFoundException;
import il.ac.jmc.matchday.repository.FavoriteTeamRepository;
import il.ac.jmc.matchday.repository.LeagueTeamRepository;
import il.ac.jmc.matchday.repository.TeamRepository;
import il.ac.jmc.matchday.util.NameFormatter;
import il.ac.jmc.matchday.util.Pagination;
import il.ac.jmc.matchday.util.QueryFilters;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing {@link il.ac.jmc.matchday.entity.Team} records.
 * All persistence operations are transactional.
 */
@Service
@Transactional
public class TeamService {

    /** Team data repository. */
    private final TeamRepository teamRepository;
    /** Player service used for delete guards. */
    private final PlayerService  playerService;
    /** Match service used for delete guards. */
    private final MatchService   matchService;
    /** League service used for membership validation. */
    private final LeagueService  leagueService;
    /** Favorite-team repository. */
    private final FavoriteTeamRepository favoriteTeamRepository;
    /** League membership repository. */
    private final LeagueTeamRepository leagueTeamRepository;

    /**
     * Creates the service with repositories and related domain services.
     *
     * @param teamRepository team repository
     * @param playerService player service used for delete guards
     * @param matchService match service used for delete guards
     * @param leagueService league service used for membership validation
     * @param favoriteTeamRepository favorite-team repository
     * @param leagueTeamRepository league membership repository
     */
    public TeamService(TeamRepository teamRepository,
                       PlayerService playerService,
                       MatchService matchService,
                       LeagueService leagueService,
                       FavoriteTeamRepository favoriteTeamRepository,
                       LeagueTeamRepository leagueTeamRepository) {
        this.teamRepository = teamRepository;
        this.playerService  = playerService;
        this.matchService   = matchService;
        this.leagueService  = leagueService;
        this.favoriteTeamRepository = favoriteTeamRepository;
        this.leagueTeamRepository = leagueTeamRepository;
    }

    /**
     * Returns all teams in the league.
     *
     * @return all teams sorted by name
     */
    public List<Team> findAll() {
        return teamRepository.findAllByOrderByNameAsc();
    }

    /**
     * Returns the team with the given primary key, or empty if not found.
     *
     * @param id team id
     * @return matching team, if present
     */
    public Optional<Team> findById(Long id) {
        return teamRepository.findById(id);
    }

    /**
     * Returns the team with the given id, or throws {@link EntityNotFoundException} (→ HTTP 404)
     * if it does not exist. Use this in controllers instead of duplicating the orElseThrow pattern.
     *
     * @param id team id
     * @return matching team
     */
    @Transactional(readOnly = true)
    public Team getByIdOrThrow(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Team.class));
    }

    /**
     * Validates uniqueness of the team name and existence of the optionally selected league,
     * adding binding errors when violated. Call after standard {@code @Valid} validation.
     *
     * @param form submitted team form
     * @param result validation result to receive business-rule errors
     * @param editId the ID of the team being edited, or {@code null} for a new team
     */
    @Transactional(readOnly = true)
    public void validateForm(TeamForm form, BindingResult result, Long editId) {
        if (!result.hasFieldErrors("name") && form.getName() != null) {
            String trimmed = form.getName().trim();
            boolean duplicate = editId != null
                    ? teamRepository.existsByNameIgnoreCaseAndIdNot(trimmed, editId)
                    : teamRepository.existsByNameIgnoreCase(trimmed);
            if (duplicate) {
                result.rejectValue("name", "duplicate", "A team with this name already exists");
            }
        }
        if (form.getLeagueId() != null && leagueService.findById(form.getLeagueId()).isEmpty()) {
            result.rejectValue("leagueId", "missing", "Selected league was not found");
        }
    }

    /**
     * Builds a pre-populated form DTO from an existing team entity.
     *
     * @param team source team
     * @return populated form
     */
    public TeamForm buildForm(Team team) {
        TeamForm form = new TeamForm();
        form.setName(team.getName());
        form.setDescription(team.getDescription());
        form.setLogoUrl(team.getLogoUrl());
        return form;
    }

    /**
     * Creates and persists a team from a validated form and, when a league is selected,
     * adds it to that league in the same transaction.
     *
     * @param form validated team form
     * @return saved team
     */
    @Transactional
    public Team createFromForm(TeamForm form) {
        Team team = new Team(form.getName().trim());
        team.setDescription(form.getDescription());
        team.setLogoUrl(form.getLogoUrl());
        team = save(team);
        if (form.getLeagueId() != null) {
            leagueService.addTeam(form.getLeagueId(), team.getId());
        }
        return team;
    }

    /**
     * Updates and persists an existing team from a validated form.
     *
     * @param team existing team
     * @param form validated team form
     */
    public void updateFromForm(Team team, TeamForm form) {
        team.setName(form.getName() != null ? form.getName().trim() : "");
        team.setDescription(form.getDescription());
        team.setLogoUrl(form.getLogoUrl());
        save(team);
    }

    /**
     * Persists or updates the given team and returns the managed instance.
     *
     * @param team team to save
     * @return saved team
     */
    public Team save(Team team) {
        if (team.getName() != null) {
            team.setName(NameFormatter.toTitleCase(team.getName()));
        }
        return teamRepository.save(team);
    }

    /**
     * Returns one page of team summaries filtered by optional name/description text
     * and optional league membership, sorted alphabetically by team name.
     *
     * @param name optional name/description filter
     * @param leagueIds optional league filters
     * @param page zero-based page number
     * @return page of team summaries
     */
    @Transactional(readOnly = true)
    public Page<TeamSummary> findSummariesPage(String name, List<Long> leagueIds, int page) {
        return Pagination.page(page, pageable -> findSummaries(name, leagueIds, pageable));
    }

    /**
     * Returns the first {@code limit} team summaries matching the filters plus the total count.
     *
     * @param name optional name/description filter
     * @param leagueIds optional league filters
     * @param limit maximum rows to return
     * @return page containing the first matching summaries
     */
    @Transactional(readOnly = true)
    public Page<TeamSummary> findTopSummaries(String name, List<Long> leagueIds, int limit) {
        return findSummaries(name, leagueIds, Pagination.first(limit));
    }

    /**
     * Executes the filtered team summary query against the repository.
     *
     * @param name optional name/description filter (blank means no filter)
     * @param leagueIds optional league id filter (empty means no filter)
     * @param pageable pagination and sort specification
     * @return page of matching team summaries
     */
    private Page<TeamSummary> findSummaries(String name, List<Long> leagueIds, Pageable pageable) {
        boolean nameFilterEmpty   = name == null || name.isBlank();
        boolean leagueFilterEmpty = leagueIds == null || leagueIds.isEmpty();
        return teamRepository.findSummariesPageable(
                nameFilterEmpty ? "" : name.trim(),
                nameFilterEmpty,
                QueryFilters.idsOrSentinel(leagueIds),
                leagueFilterEmpty,
                pageable);
    }

    /**
     * Returns the total number of teams.
     *
     * @return team count
     */
    public long count() {
        return teamRepository.count();
    }

    /**
     * Returns the number of teams missing a logo URL.
     *
     * @return count of teams without logo URLs
     */
    public long countMissingLogos() {
        return teamRepository.countMissingLogoUrls();
    }

    /**
     * Deletes the team and all its league memberships atomically.
     * Throws {@link IllegalStateException} with a user-readable message
     * if the team still has players or associated matches.
     *
     * @param id team id to delete
     */
    @Transactional
    public void deleteById(Long id) {
        if (playerService.hasPlayersInTeam(id)) {
            throw new IllegalStateException(
                    "Cannot delete \"" + displayName(id) + "\" - remove all players first.");
        }
        if (matchService.hasMatchesForTeam(id)) {
            throw new IllegalStateException(
                    "Cannot delete \"" + displayName(id) + "\" - it has associated matches.");
        }
        favoriteTeamRepository.deleteByTeamId(id);
        leagueTeamRepository.deleteByTeamId(id);
        teamRepository.deleteById(id);
    }

    /**
     * Returns a human-readable team name for use in error messages.
     *
     * @param id team id
     * @return team name, or {@code "this team"} if the team cannot be found
     */
    private String displayName(Long id) {
        return teamRepository.findById(id).map(Team::getName).orElse("this team");
    }
}
