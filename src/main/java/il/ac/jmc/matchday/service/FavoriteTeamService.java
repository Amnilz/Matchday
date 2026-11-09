package il.ac.jmc.matchday.service;

import il.ac.jmc.matchday.entity.FavoriteTeam;
import il.ac.jmc.matchday.entity.Team;
import il.ac.jmc.matchday.entity.User;
import il.ac.jmc.matchday.exception.EntityNotFoundException;
import il.ac.jmc.matchday.repository.FavoriteTeamRepository;
import il.ac.jmc.matchday.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Service for managing a user's saved favorite teams.
 */
@Service
@Transactional
public class FavoriteTeamService {

    /** Repository for favorite team records. */
    private final FavoriteTeamRepository favoriteTeamRepository;
    /** Service for user lookups. */
    private final UserService userService;
    /** Repository for team records. */
    private final TeamRepository teamRepository;

    /**
     * Creates the service with repositories and user lookup service.
     *
     * @param favoriteTeamRepository favorite-team repository
     * @param userService user lookup service
     * @param teamRepository team repository
     */
    public FavoriteTeamService(FavoriteTeamRepository favoriteTeamRepository,
                               UserService userService,
                               TeamRepository teamRepository) {
        this.favoriteTeamRepository = favoriteTeamRepository;
        this.userService = userService;
        this.teamRepository = teamRepository;
    }

    /**
     * Saves the given team for the user unless it is already saved.
     *
     * @param username username owning the favorite
     * @param teamId team id to favorite
     * @return favorited team
     */
    public Team addFavorite(String username, Long teamId) {
        User user = requireUser(username);
        Team team = requireTeam(teamId);
        if (favoriteTeamRepository.existsByUserIdAndTeamId(user.getId(), team.getId())) return team;
        favoriteTeamRepository.save(new FavoriteTeam(user, team));
        return team;
    }

    /**
     * Removes the given team from the user's favorites; absent rows are ignored.
     *
     * @param username username owning the favorite
     * @param teamId team id to remove
     * @return removed team
     */
    public Team removeFavorite(String username, Long teamId) {
        User user = requireUser(username);
        Team team = requireTeam(teamId);
        favoriteTeamRepository.deleteByUserIdAndTeamId(user.getId(), teamId);
        return team;
    }

    /**
     * Returns true when the logged-in user already saved the team.
     *
     * @param username username to check
     * @param teamId team id to check
     * @return true when the team is a favorite
     */
    @Transactional(readOnly = true)
    public boolean isFavorite(String username, Long teamId) {
        return userService.findByUsername(username)
                .map(user -> favoriteTeamRepository.existsByUserIdAndTeamId(user.getId(), teamId))
                .orElse(false);
    }

    /**
     * Returns all saved teams for the given user, newest first.
     *
     * @param username username to load favorites for
     * @return favorite teams
     */
    @Transactional(readOnly = true)
    public List<Team> findFavoriteTeams(String username) {
        User user = requireUser(username);
        return favoriteTeamRepository.findByUserIdWithTeamOrderByCreatedAtDesc(user.getId()).stream()
                .map(FavoriteTeam::getTeam)
                .toList();
    }

    /**
     * Returns all favorite team IDs for the given user.
     *
     * @param username username to load favorite ids for
     * @return favorite team ids
     */
    @Transactional(readOnly = true)
    public Set<Long> findFavoriteTeamIds(String username) {
        return userService.findByUsername(username)
                .map(user -> Set.copyOf(favoriteTeamRepository.findTeamIdsByUserId(user.getId())))
                .orElse(Set.of());
    }

    /**
     * Returns favorite team IDs for the given username, or an empty set when
     * {@code username} is {@code null} (unauthenticated visitor).
     *
     * @param username username to load favorite ids for, or {@code null}
     * @return favorite team ids, or an empty set for anonymous users
     */
    @Transactional(readOnly = true)
    public Set<Long> findFavoriteTeamIdsOrEmpty(String username) {
        if (username == null) return Set.of();
        return findFavoriteTeamIds(username);
    }

    /**
     * Returns the user with the given username, or throws if not found.
     *
     * @param username username to look up
     * @return matching user
     */
    private User requireUser(String username) {
        return userService.getByUsernameOrThrow(username);
    }

    /**
     * Returns the team with the given id, or throws if not found.
     *
     * @param teamId team id to look up
     * @return matching team
     */
    private Team requireTeam(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new EntityNotFoundException(Team.class));
    }
}
