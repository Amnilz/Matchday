package il.ac.jmc.matchday.security;

import il.ac.jmc.matchday.entity.User;
import il.ac.jmc.matchday.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.jspecify.annotations.NonNull;

/**
 * Spring Security UserDetailsService that loads a User from the database by username.
 * Roles are stored in the DB with the ROLE_ prefix and mapped directly to authorities.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    /** Repository used to load application users for Spring Security. */
    private final UserRepository userRepository;

    /**
     * Creates a user details service backed by the application user repository.
     *
     * @param userRepository repository used to find users by username or email
     */
    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads a Spring Security user by username or email.
     *
     * @param username submitted username or email address
     * @return Spring Security user details with database-backed authorities
     * @throws UsernameNotFoundException if no matching user exists
     */
    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String username)
            throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(user.getRoles().stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList())
                .build();
    }
}
