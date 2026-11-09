package il.ac.jmc.matchday.service;

import il.ac.jmc.matchday.config.Roles;
import il.ac.jmc.matchday.entity.User;
import il.ac.jmc.matchday.exception.EntityNotFoundException;
import il.ac.jmc.matchday.repository.UserRepository;
import il.ac.jmc.matchday.util.Pagination;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for managing {@link il.ac.jmc.matchday.entity.User} records.
 * All persistence operations are transactional.
 */
@Service
@Transactional
public class UserService {

    /** User data repository. */
    private final UserRepository userRepository;

    /**
     * Creates the service with the user repository.
     *
     * @param userRepository user repository
     */
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns one page of users ordered for admin display.
     *
     * @param page zero-based page number
     * @return user page
     */
    @Transactional(readOnly = true)
    public Page<User> findAdminPage(int page) {
        return Pagination.page(page, userRepository::findAllByOrderByUsernameAsc);
    }

    /**
     * Returns the user with the given username, or empty if not found.
     *
     * @param username username to search for
     * @return matching user, if present
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Returns the user behind an authenticated principal.
     * Throws when the account no longer exists (deleted mid-session).
     *
     * @param username authenticated username
     * @return matching user
     */
    @Transactional(readOnly = true)
    public User getByUsernameOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    /**
     * Returns the total number of users.
     *
     * @return user count
     */
    public long count() {
        return userRepository.count();
    }

    /**
     * Blocks the given user from comment actions.
     *
     * @param id user id to block
     * @param actorUsername username performing the action
     * @return blocked user
     */
    public User blockUser(Long id, String actorUsername) {
        User user = requireUser(id);
        if (user.getUsername().equals(actorUsername)) {
            throw new IllegalArgumentException("You cannot block your own account.");
        }
        if (user.getRoles().contains(Roles.ADMIN)) {
            throw new IllegalArgumentException("Admin accounts cannot be blocked.");
        }
        user.setBlocked(true);
        return user;
    }

    /**
     * Removes the comment block from the given user.
     *
     * @param id user id to unblock
     * @return unblocked user
     */
    public User unblockUser(Long id) {
        User user = requireUser(id);
        user.setBlocked(false);
        return user;
    }

    /**
     * Returns the user with the given id, or throws {@link il.ac.jmc.matchday.exception.EntityNotFoundException}
     * (HTTP 404) if no such user exists.
     *
     * @param id user id
     * @return matching user
     * @throws il.ac.jmc.matchday.exception.EntityNotFoundException if the user is not found
     */
    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(User.class));
    }
}
