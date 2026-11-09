package il.ac.jmc.matchday.controller.admin;

/**
 * Model attribute names shared by the admin CRUD controllers.
 */
final class AdminModelAttributes {

    /** ID of the entity being edited; {@code null} on create forms. */
    static final String EDIT_ID = "editId";

    /**
     * Utility class; not instantiable.
     */
    private AdminModelAttributes() {}
}
