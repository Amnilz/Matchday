package il.ac.jmc.matchday.dto;

/**
 * JSON response for the /admin/wiki-status polling endpoint.
 * Jackson serializes this automatically - no manual string building needed.
 *
 * @param running whether a Wikipedia fetch is currently in progress
 * @param type    which fetch finished last: "teams", "leagues" or "players"
 * @param message completion message to flash to the admin, or {@code null}
 */
public record WikiStatusResponse(boolean running, String type, String message) {}
