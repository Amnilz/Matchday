package il.ac.jmc.matchday.security;

import il.ac.jmc.matchday.util.ReturnUrlUtil;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * After a successful login, redirects to the {@code returnUrl} form parameter when present
 * and safe (relative path only). Falls back to Spring Security's saved-request mechanism,
 * then to the default URL ("/").
 */
@Component
public class ReturnUrlSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    /**
     * Sets the default post-login target to the home page after construction.
     */
    @PostConstruct
    public void configureDefaultTargetUrl() {
        setDefaultTargetUrl("/");
    }

    /**
     * Redirects to a safe {@code returnUrl} parameter or delegates to Spring Security defaults.
     *
     * @param request current HTTP request
     * @param response current HTTP response
     * @param authentication successful authentication
     * @throws IOException if redirecting fails
     * @throws ServletException if the delegated success handler fails
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        @NonNull HttpServletResponse response,
                                        @NonNull Authentication authentication) throws IOException, ServletException {
        String returnUrl = request.getParameter("returnUrl");
        if (ReturnUrlUtil.isSafe(returnUrl)) {
            clearAuthenticationAttributes(request);
            getRedirectStrategy().sendRedirect(request, response, returnUrl);
        } else {
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }
}
