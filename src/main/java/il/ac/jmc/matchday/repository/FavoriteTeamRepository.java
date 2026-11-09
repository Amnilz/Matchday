package il.ac.jmc.matchday.repository;

import il.ac.jmc.matchday.entity.FavoriteTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for saved favorite teams.
 */
@Repository
public interface FavoriteTeamRepository extends JpaRepository<FavoriteTeam, Long> {

    /**
     * Returns a user's favorite teams with the team fetched for display.
     *
     * @param userId the ID of the user
     * @return a list of favorite teams ordered by creation date descending
     */
    @Query("""
            SELECT f FROM FavoriteTeam f
            JOIN FETCH f.team
            WHERE f.user.id = :userId
            ORDER BY f.createdAt DESC
            """)
    List<FavoriteTeam> findByUserIdWithTeamOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * Returns IDs of all teams saved by the given user.
     *
     * @param userId the ID of the user
     * @return a list of team IDs
     */
    @Query("""
            SELECT f.team.id FROM FavoriteTeam f
            WHERE f.user.id = :userId
            """)
    List<Long> findTeamIdsByUserId(@Param("userId") Long userId);

    /**
     * Returns true when the user already saved the team.
     *
     * @param userId the ID of the user
     * @param teamId the ID of the team
     * @return true if the favorite exists, false otherwise
     */
    boolean existsByUserIdAndTeamId(Long userId, Long teamId);

    /**
     * Removes a saved team for a user; absent rows are a no-op.
     *
     * @param userId the ID of the user
     * @param teamId the ID of the team
     */
    void deleteByUserIdAndTeamId(Long userId, Long teamId);

    /**
     * Removes all saved favorites for a team that is being deleted.
     *
     * @param teamId the ID of the team
     */
    void deleteByTeamId(Long teamId);
}
