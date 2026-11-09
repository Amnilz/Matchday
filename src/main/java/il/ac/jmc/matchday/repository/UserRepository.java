package il.ac.jmc.matchday.repository;

import il.ac.jmc.matchday.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link il.ac.jmc.matchday.entity.User} entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Returns the user with the given username, if one exists.
     *
     * @param username the username to search for
     * @return an Optional containing the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Returns the user with the given email address, if one exists.
     *
     * @param email the email address to search for
     * @return an Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Returns one page of users ordered for admin display.
     *
     * @param pageable pagination details
     * @return a page of users ordered by username ascending
     */
    Page<User> findAllByOrderByUsernameAsc(Pageable pageable);

    /**
     * Returns {@code true} if a user with the given username is already stored.
     *
     * @param username the username to check
     * @return true if the username exists, false otherwise
     */
    boolean existsByUsername(String username);

    /**
     * Returns {@code true} if a user with the given email address is already stored.
     *
     * @param email the email address to check
     * @return true if the email exists, false otherwise
     */
    boolean existsByEmail(String email);
}
