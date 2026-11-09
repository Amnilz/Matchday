package il.ac.jmc.matchday.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Injects global model attributes available in every Thymeleaf template.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    /**
     * Creates the global model advice component.
     */
    public GlobalModelAdvice() {}

    /**
     * Exposes {@code loginReturnUrl} so every page's Login link returns the user here.
     *
     * @param model MVC model populated for every controller response
     * @param request current HTTP request used to reconstruct the return URL
     */
    @ModelAttribute
    public void addGlobalAttributes(Model model, HttpServletRequest request) {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        model.addAttribute("loginReturnUrl", query != null ? uri + "?" + query : uri);
    }
}
