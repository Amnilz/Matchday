package il.ac.jmc.matchday.service;

import il.ac.jmc.matchday.config.Roles;
import il.ac.jmc.matchday.dto.RegistrationForm;
import il.ac.jmc.matchday.entity.User;
import il.ac.jmc.matchday.repository.UserRepository;
import il.ac.jmc.matchday.validation.RegistrationConstraints;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import java.util.Set;

/**
 * Handles new-user registration: validation, uniqueness checks, password hashing, and persistence.
 */
@Service
public class RegistrationService {

    /** Repository for user records. */
    private final UserRepository userRepository;
    /** Encoder used to hash passwords before storage. */
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates the service with user storage and password hashing dependencies.
     *
     * @param userRepository user repository
     * @param passwordEncoder password encoder
     */
    public RegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Validates business rules not covered by Bean Validation (unique username/email,
     * password confirmation match) and adds field errors to {@code result}.
     *
     * @param form submitted registration form
     * @param result validation result to receive business-rule errors
     */
    public void validate(RegistrationForm form, BindingResult result) {
        if (userRepository.existsByUsername(form.getUsername())) {
            result.rejectValue("username", "username.taken", RegistrationConstraints.MSG_USERNAME_TAKEN);
        }
        if (userRepository.existsByEmail(form.getEmail())) {
            result.rejectValue("email", "email.taken", RegistrationConstraints.MSG_EMAIL_TAKEN);
        }
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "password.mismatch", RegistrationConstraints.MSG_PASSWORD_MISMATCH);
        }
    }

    /**
     * Creates and persists a new USER-role account from the validated form.
     *
     * @param form validated registration form
     */
    @Transactional
    public void register(RegistrationForm form) {
        User user = new User(
                form.getUsername(),
                form.getEmail(),
                passwordEncoder.encode(form.getPassword()),
                Set.of(Roles.USER)
        );
        userRepository.save(user);
    }
}
