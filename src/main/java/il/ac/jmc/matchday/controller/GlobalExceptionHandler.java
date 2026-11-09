package il.ac.jmc.matchday.controller;

import il.ac.jmc.matchday.exception.CommentNotAllowedException;
import il.ac.jmc.matchday.exception.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Renders application error pages for common controller exceptions.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /** Logger for unhandled exceptions. */
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Creates the global exception handler.
     */
    public GlobalExceptionHandler() {}

    /**
     * Renders 404 for requests to non-existent static resources or URLs.
     *
     * @return 404 model and view
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ModelAndView handleNoResourceFound() {
        return errorView(HttpStatus.NOT_FOUND, defaultMessage(HttpStatus.NOT_FOUND));
    }

    /**
     * Renders the page matching an explicitly thrown {@link ResponseStatusException}.
     *
     * @param ex response status exception raised by a controller or framework
     * @return model and view matching the exception status
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ModelAndView handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = resolveStatus(ex.getStatusCode().value());
        return errorView(status, messageOrDefault(status, ex.getReason()));
    }

    /**
     * Renders 404 when a path variable cannot be converted.
     *
     * @return 404 model and view
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ModelAndView handleTypeMismatch() {
        return errorView(HttpStatus.NOT_FOUND, defaultMessage(HttpStatus.NOT_FOUND));
    }

    /**
     * Renders 404 when a service reports that a requested entity does not exist.
     *
     * @param ex entity-not-found exception
     * @return 404 model and view
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ModelAndView handleEntityNotFound(EntityNotFoundException ex) {
        return errorView(HttpStatus.NOT_FOUND, messageOrDefault(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    /**
     * Renders 400 with the rule that blocked the comment action.
     *
     * @param ex comment rule exception
     * @return 400 model and view
     */
    @ExceptionHandler(CommentNotAllowedException.class)
    public ModelAndView handleCommentNotAllowed(CommentNotAllowedException ex) {
        return errorView(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Renders 400 for invalid request arguments rejected by services.
     *
     * @param ex invalid argument exception
     * @return 400 model and view
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ModelAndView handleIllegalArgument(IllegalArgumentException ex) {
        return errorView(HttpStatus.BAD_REQUEST, messageOrDefault(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    /**
     * Logs any unhandled exception and renders the generic 500 page.
     *
     * @param ex unhandled exception
     * @return 500 model and view
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneral(Exception ex) {
        log.error("Unhandled controller exception", ex);
        return errorView(HttpStatus.INTERNAL_SERVER_ERROR, defaultMessage(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    /**
     * Builds a ModelAndView for the given HTTP status and message.
     *
     * @param status HTTP status to render
     * @param message error message to display
     * @return configured model and view
     */
    private ModelAndView errorView(HttpStatus status, String message) {
        ModelAndView modelAndView = new ModelAndView(viewName(status));
        modelAndView.setStatus(status);
        modelAndView.addObject("message", message);
        return modelAndView;
    }

    /**
     * Returns the Thymeleaf template name for the given HTTP status.
     *
     * @param status HTTP status
     * @return template name
     */
    private String viewName(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "error/400";
            case FORBIDDEN -> "error/403";
            case NOT_FOUND -> "error/404";
            default -> status.is4xxClientError() ? "error/400" : "error/500";
        };
    }

    /**
     * Resolves a numeric status code to an {@link HttpStatus}, defaulting to 500.
     *
     * @param statusCode numeric HTTP status code
     * @return resolved status
     */
    private HttpStatus resolveStatus(int statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode);
        return status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * Returns the given message when non-blank, or the default message for the status.
     *
     * @param status HTTP status used for the fallback message
     * @param message candidate message
     * @return non-blank message
     */
    private String messageOrDefault(HttpStatus status, String message) {
        return (message == null || message.isBlank()) ? defaultMessage(status) : message;
    }

    /**
     * Returns a generic user-facing message for common HTTP statuses.
     *
     * @param status HTTP status
     * @return default error message
     */
    private String defaultMessage(HttpStatus status) {
        return switch (status) {
            case FORBIDDEN -> "You do not have permission to access this page.";
            case NOT_FOUND -> "The page or item you are looking for does not exist or has been removed.";
            default -> "The request could not be completed.";
        };
    }

}
