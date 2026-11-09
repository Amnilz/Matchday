package il.ac.jmc.matchday.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA entity representing a registered user of the Matchday application.
 * Stores credentials, roles, and account status.
 */
@Entity
@Table(name = "users")
public class User {

    /** Maximum username length. */
    public static final int USERNAME_MAX = 20;

    /** Maximum email length. */
    public static final int EMAIL_MAX = 254;

    /** BCrypt hash length stored in the database. */
    public static final int PASSWORD_HASH_MAX = 60;

    /** Maximum role authority string length. */
    public static final int ROLE_MAX = 20;

    /** Database identifier. Generated automatically by the database when a new user is inserted. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique username used for login. Cannot be null and has a maximum length defined by {@link #USERNAME_MAX}. */
    @Column(unique = true, nullable = false, length = USERNAME_MAX)
    private String username;

    /** Unique email address. Cannot be null and has a maximum length defined by {@link #EMAIL_MAX}. */
    @Column(unique = true, nullable = false, length = EMAIL_MAX)
    private String email;

    /** BCrypt-hashed password. Cannot be null and has a fixed length defined by {@link #PASSWORD_HASH_MAX}. */
    @Column(nullable = false, length = PASSWORD_HASH_MAX)
    private String passwordHash;

    /** Set of role authority strings (e.g. "ROLE_USER", "ROLE_ADMIN"). Stored in a separate table "user_roles" with a foreign key to the user. Eagerly fetched for authentication purposes. Each role string cannot be null and has a maximum length defined by {@link #ROLE_MAX}. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false, length = ROLE_MAX)
    private Set<String> roles = new HashSet<>();

    /** Whether the user is blocked from performing comment/report actions. Defaults to false. Cannot be null. */
    @Column(nullable = false)
    private boolean blocked = false;

    /** Timestamp when the user was created. Set automatically when the user is first persisted and never updated. */
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /** Timestamp when the user was last updated. Set automatically whenever the user is updated. */
    private LocalDateTime updatedAt;


    /**
     * JPA-only constructor.
     */
    protected User() {}

    /**
     * Creates a new user with the given credentials and roles.
     *
     * @param username     unique login name
     * @param email        unique email address
     * @param passwordHash Bcrypt-hashed password
     * @param roles        set of authority strings (e.g. {@code ROLE_USER})
     */
    public User(String username, String email, String passwordHash, Set<String> roles) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roles = (roles != null) ? new HashSet<>(roles) : new HashSet<>();
    }

    /**
     * Initializes audit timestamps before the user is first persisted.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    /**
     * Refreshes the update timestamp before the user is saved.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Returns the database id.
     *
     * @return user id
     */
    public Long getId() { return id; }

    /**
     * Returns the unique username.
     *
     * @return username
     */
    public String getUsername() { return username; }

    /**
     * Sets the unique username.
     *
     * @param username username
     */
    public void setUsername(String username) { this.username = username; }

    /**
     * Returns the unique email address.
     *
     * @return email address
     */
    public String getEmail() { return email; }

    /**
     * Sets the unique email address.
     *
     * @param email email address
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * Returns the BCrypt password hash.
     *
     * @return password hash
     */
    public String getPasswordHash() { return passwordHash; }

    /**
     * Sets the BCrypt password hash.
     *
     * @param passwordHash password hash
     */
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    /**
     * Returns the user's role authorities.
     *
     * @return role authority strings
     */
    public Set<String> getRoles() { return roles; }

    /**
     * Replaces the user's role authorities.
     *
     * @param roles role authority strings
     */
    public void setRoles(Set<String> roles) {
        this.roles = (roles != null) ? new HashSet<>(roles) : new HashSet<>();
    }

    /**
     * Returns whether the user is blocked from comment/report actions.
     *
     * @return true when blocked
     */
    public boolean isBlocked() { return blocked; }

    /**
     * Sets whether the user is blocked from comment/report actions.
     *
     * @param blocked true when blocked
     */
    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    /**
     * Returns when the user was created.
     *
     * @return creation timestamp
     */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /**
     * Returns when the user was last updated.
     *
     * @return update timestamp
     */
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
