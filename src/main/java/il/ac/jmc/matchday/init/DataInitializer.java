package il.ac.jmc.matchday.init;

import il.ac.jmc.matchday.config.Roles;
import il.ac.jmc.matchday.entity.User;
import il.ac.jmc.matchday.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/** Creates an administrator only when explicitly enabled with a local password. */
@Component
@ConditionalOnProperty(name = "app.admin.enabled", havingValue = "true")
public class DataInitializer implements ApplicationRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           @Value("${app.admin.username}") String adminUsername,
                           @Value("${app.admin.email}") String adminEmail,
                           @Value("${app.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    /** Existing accounts are never overwritten; changing .env does not reset a password. */
    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) {
        if (userRepository.existsByUsername(adminUsername)) return;
        if (adminPassword.isBlank() || adminPassword.length() < 12
                || adminPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(
                    "Set ADMIN_PASSWORD to at least 12 characters (maximum 72 UTF-8 bytes), "
                    + "or set ADMIN_ENABLED=false.");
        }
        User admin = new User(adminUsername, adminEmail,
                passwordEncoder.encode(adminPassword), Set.of(Roles.ADMIN, Roles.USER));
        userRepository.save(admin);
    }
}
