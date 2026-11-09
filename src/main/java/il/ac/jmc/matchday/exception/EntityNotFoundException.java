package il.ac.jmc.matchday.exception;

import java.io.Serial;

/**
 * Thrown by the service layer when a requested entity does not exist.
 * Mapped to HTTP 404 by {@link il.ac.jmc.matchday.controller.GlobalExceptionHandler}.
 */
public class EntityNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new exception indicating that an entity of the specified type was not found.
     * @param entityType the type of the missing entity, used to generate a descriptive error message
     */
    public EntityNotFoundException(Class<?> entityType) {
        super(entityType.getSimpleName() + " not found");
    }
}
