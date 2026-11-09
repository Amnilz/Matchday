package il.ac.jmc.matchday.dto;

import il.ac.jmc.matchday.validation.RegistrationConstraints;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Form-backing DTO for the user registration page.
 * Never bound directly to the User entity.
 */
public class RegistrationForm {

    /**
     * Creates an empty registration form for MVC binding.
     */
    public RegistrationForm() {}

    /** Submitted username. */
    @NotBlank(message = RegistrationConstraints.MSG_USERNAME_REQUIRED)
    @Size(min = RegistrationConstraints.USERNAME_MIN, max = RegistrationConstraints.USERNAME_MAX,
          message = RegistrationConstraints.MSG_USERNAME_SIZE)
    @Pattern(regexp = RegistrationConstraints.USERNAME_PATTERN, message = RegistrationConstraints.MSG_USERNAME_PATTERN)
    private String username;

    /** Submitted email address. */
    @NotBlank(message = RegistrationConstraints.MSG_EMAIL_REQUIRED)
    @Email(message = RegistrationConstraints.MSG_EMAIL_INVALID)
    @Size(max = RegistrationConstraints.EMAIL_MAX, message = RegistrationConstraints.MSG_EMAIL_MAX)
    private String email;

    /** Submitted plain-text password. */
    @NotBlank(message = RegistrationConstraints.MSG_PASSWORD_REQUIRED)
    @Size.List({
        @Size(min = RegistrationConstraints.PASSWORD_MIN, message = RegistrationConstraints.MSG_PASSWORD_MIN),
        @Size(max = RegistrationConstraints.PASSWORD_MAX, message = RegistrationConstraints.MSG_PASSWORD_MAX)
    })
    private String password;

    /** Submitted password confirmation. */
    @NotBlank(message = RegistrationConstraints.MSG_CONFIRM_REQUIRED)
    private String confirmPassword;

    /**
     * Returns the submitted username.
     *
     * @return username
     */
    public String getUsername() { return username; }

    /**
     * Sets the submitted username.
     *
     * @param username username
     */
    public void setUsername(String username) { this.username = username; }

    /**
     * Returns the submitted email address.
     *
     * @return email address
     */
    public String getEmail() { return email; }

    /**
     * Sets the submitted email address.
     *
     * @param email email address
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * Returns the submitted plain-text password.
     *
     * @return plain-text password
     */
    public String getPassword() { return password; }

    /**
     * Sets the submitted plain-text password.
     *
     * @param password plain-text password
     */
    public void setPassword(String password) { this.password = password; }

    /**
     * Returns the submitted password confirmation.
     *
     * @return password confirmation
     */
    public String getConfirmPassword() { return confirmPassword; }

    /**
     * Sets the submitted password confirmation.
     *
     * @param confirmPassword password confirmation
     */
    @SuppressWarnings("unused") // invoked by Spring MVC form binding (th:field="*{confirmPassword}")
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
