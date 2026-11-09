package il.ac.jmc.matchday.validation;

import il.ac.jmc.matchday.entity.User;

/**
 * Compile-time constants for registration field validation.
 * Referenced in @Size annotations and passed to the registration template.
 * Username and email maxima delegate to {@link User} so form limits mirror DB columns.
 */
public final class RegistrationConstraints {

    /** Minimum username length. */
    public static final int USERNAME_MIN = 3;
    /** Maximum username length, mirrors the DB column. */
    public static final int USERNAME_MAX = User.USERNAME_MAX;
    /** Minimum password length. */
    public static final int PASSWORD_MIN = 6;
    /** Maximum password length (BCrypt input limit). */
    public static final int PASSWORD_MAX = 72;
    /** Maximum email length, mirrors the DB column. */
    public static final int EMAIL_MAX    = User.EMAIL_MAX;

    /** Allowed username shape: starts with a letter, then letters/digits/'-'/'_'. */
    public static final String USERNAME_PATTERN = "^[a-zA-Z][a-zA-Z0-9_-]*$";

    /** Shown when the username field is empty. */
    public static final String MSG_USERNAME_REQUIRED     = "Username is required";
    /** Shown when the username length is out of range. */
    public static final String MSG_USERNAME_SIZE         = "Must be " + USERNAME_MIN + "–" + USERNAME_MAX + " characters";
    /** Shown when the username contains disallowed characters. */
    public static final String MSG_USERNAME_PATTERN      = "Must start with a letter. Only a-z, A-Z, 0-9, '-', '_' allowed";
    /** Shown when the email field is empty. */
    public static final String MSG_EMAIL_REQUIRED        = "Email is required";
    /** Shown when the email is not a valid address. */
    public static final String MSG_EMAIL_INVALID         = "Must be a valid email address";
    /** Shown when the email exceeds the maximum length. */
    public static final String MSG_EMAIL_MAX             = "Must be at most " + EMAIL_MAX + " characters";
    /** Shown when the password field is empty. */
    public static final String MSG_PASSWORD_REQUIRED     = "Password is required";
    /** Shown when the password is too short. */
    public static final String MSG_PASSWORD_MIN          = "Must be at least " + PASSWORD_MIN + " characters";
    /** Shown when the password is too long. */
    public static final String MSG_PASSWORD_MAX          = "Must be at most " + PASSWORD_MAX + " characters";
    /** Shown when the confirm-password field is empty. */
    public static final String MSG_CONFIRM_REQUIRED      = "Please confirm your password";
    /** Shown when the username already belongs to another account. */
    public static final String MSG_USERNAME_TAKEN        = "Username is already taken";
    /** Shown when the email already belongs to another account. */
    public static final String MSG_EMAIL_TAKEN           = "Email is already registered";
    /** Shown when password and confirmation differ. */
    public static final String MSG_PASSWORD_MISMATCH     = "Passwords did not match. Please type them again.";

    /** Prevents instantiation of this constants class. */
    private RegistrationConstraints() {}
}
