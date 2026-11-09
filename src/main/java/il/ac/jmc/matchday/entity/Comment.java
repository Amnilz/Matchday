package il.ac.jmc.matchday.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**JPA entity representing a registered user's comment on a match. */
@Entity
@Table(name = "comments")
public class Comment {

    /** Maximum length of a stored comment body. */
    public static final int CONTENT_MAX = 500;

    /** Database identifier. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Match that owns this comment thread entry. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    /** User who authored the comment. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Parent comment when this entry is a reply. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    /** Stored comment body or deletion placeholder text. */
    @Column(nullable = false, length = CONTENT_MAX)
    private String content;

    /** Whether the comment is visible in public threads. */
    @Column(nullable = false)
    private boolean visible = true;

    /** Whether the comment is waiting for admin review. */
    @Column(nullable = false)
    private boolean reported = false;

    /** Whether the comment has been soft-deleted. */
    @Column(nullable = false)
    private boolean deleted = false;

    /** Timestamp when the comment was created. */
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    /** Timestamp when the comment was last updated. */
    private LocalDateTime updatedAt;

    /**
     * JPA-only constructor.
     */
    protected Comment() {}

    /**
     * Creates a top-level comment for a match.
     *
     * @param match commented match
     * @param user comment author
     * @param content comment body
     */
    public Comment(Match match, User user, String content) {
        this.match = match;
        this.user = user;
        this.content = content;
    }

    /**
     * Initializes audit timestamps before the comment is first persisted.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    /**
     * Refreshes the update timestamp before the comment is saved.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Returns the database id.
     *
     * @return comment id
     */
    public Long getId() { return id; }

    /**
     * Returns the match this comment belongs to.
     *
     * @return commented match
     */
    public Match getMatch() { return match; }

    /**
     * Sets the match this comment belongs to.
     *
     * @param match commented match
     */
    public void setMatch(Match match) { this.match = match; }

    /**
     * Returns the comment author.
     *
     * @return comment author
     */
    public User getUser() { return user; }

    /**
     * Sets the comment author.
     *
     * @param user comment author
     */
    public void setUser(User user) { this.user = user; }

    /**
     * Returns the parent comment for replies.
     *
     * @return parent comment, or {@code null} for top-level comments
     */
    public Comment getParentComment() { return parentComment; }

    /**
     * Sets the parent comment for replies.
     *
     * @param parentComment parent comment
     */
    public void setParentComment(Comment parentComment) { this.parentComment = parentComment; }

    /**
     * Returns the comment body.
     *
     * @return comment body
     */
    public String getContent() { return content; }

    /**
     * Sets the comment body.
     *
     * @param content comment body
     */
    public void setContent(String content) { this.content = content; }

    /**
     * Returns whether the comment is visible to regular users.
     *
     * @return true when visible
     */
    public boolean isVisible() { return visible; }

    /**
     * Sets whether the comment is visible to regular users.
     *
     * @param visible true when visible
     */
    public void setVisible(boolean visible) { this.visible = visible; }

    /**
     * Returns whether the comment has been reported.
     *
     * @return true when reported
     */
    public boolean isReported() { return reported; }

    /**
     * Sets whether the comment has been reported.
     *
     * @param reported true when reported
     */
    public void setReported(boolean reported) { this.reported = reported; }

    /**
     * Returns whether the comment has been soft-deleted.
     *
     * @return true when deleted
     */
    public boolean isDeleted() { return deleted; }

    /**
     * Sets whether the comment has been soft-deleted.
     *
     * @param deleted true when deleted
     */
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    /**
     * Returns when the comment was created.
     *
     * @return creation timestamp
     */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /**
     * Returns when the comment was last updated.
     *
     * @return update timestamp
     */
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
