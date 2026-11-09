package il.ac.jmc.matchday.repository;

import il.ac.jmc.matchday.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for match comments.
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * Counts comments that are waiting for admin review.
     *
     * @return the number of reported comments
     */
    long countByReportedTrue();

    /**
     * Detaches reply links of a match's comments so they can be bulk-deleted safely.
     *
     * @param matchId the ID of the match
     */
    @Modifying
    @Query("UPDATE Comment c SET c.parentComment = NULL WHERE c.match.id = :matchId")
    void detachRepliesByMatchId(@Param("matchId") Long matchId);

    /**
     * Deletes all comments belonging to the given match (used when a match is deleted).
     *
     * @param matchId the ID of the match
     */
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.match.id = :matchId")
    void deleteByMatchId(@Param("matchId") Long matchId);

    /**
     * Returns visible comments and deleted placeholders for building a thread.
     *
     * @param matchId the ID of the match
     * @return a list of thread candidate comments
     */
    @Query("""
            SELECT c FROM Comment c
            JOIN FETCH c.user
            LEFT JOIN FETCH c.parentComment
            WHERE c.match.id = :matchId
            AND (c.visible = TRUE OR c.deleted = TRUE)
            ORDER BY c.createdAt ASC
            """)
    List<Comment> findThreadCandidatesByMatchIdOrderByCreatedAtAsc(@Param("matchId") Long matchId);

    /**
     * Returns a comment with its author and match fetched for ownership checks.
     *
     * @param id the ID of the comment
     * @return an Optional containing the comment if found
     */
    @Query("""
            SELECT c FROM Comment c
            JOIN FETCH c.user
            JOIN FETCH c.match
            WHERE c.id = :id
            """)
    Optional<Comment> findByIdWithUserAndMatch(@Param("id") Long id);

    /**
     * Returns all comments written by a user, with match and teams fetched for profile display.
     *
     * @param userId the ID of the user
     * @return a list of comments ordered by creation date descending
     */
    @Query("""
            SELECT c FROM Comment c
            JOIN FETCH c.match m
            JOIN FETCH m.homeTeam
            JOIN FETCH m.awayTeam
            LEFT JOIN FETCH c.parentComment p
            LEFT JOIN FETCH p.user
            WHERE c.user.id = :userId
            AND c.deleted = FALSE
            ORDER BY c.createdAt DESC
            """)
    List<Comment> findByUserIdWithMatchOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * Returns one page of admin moderation comments with display data fetched.
     *
     * @param reportedOnly if true, only fetches reported comments
     * @param pageable pagination details
     * @return a page of comments
     */
    @Query(value = """
            SELECT c FROM Comment c
            JOIN FETCH c.user
            JOIN FETCH c.match m
            JOIN FETCH m.homeTeam
            JOIN FETCH m.awayTeam
            LEFT JOIN FETCH c.parentComment p
            LEFT JOIN FETCH p.user
            WHERE (:reportedOnly = FALSE OR c.reported = TRUE)
            ORDER BY CASE WHEN c.reported = TRUE THEN 0 ELSE 1 END, c.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(c) FROM Comment c
            WHERE (:reportedOnly = FALSE OR c.reported = TRUE)
            """)
    Page<Comment> findAdminCommentsPage(@Param("reportedOnly") boolean reportedOnly,
                                        Pageable pageable);
}
