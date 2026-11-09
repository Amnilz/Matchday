package il.ac.jmc.matchday.controller;

import il.ac.jmc.matchday.dto.RegistrationForm;
import il.ac.jmc.matchday.service.RegistrationService;
import il.ac.jmc.matchday.util.ReturnUrlUtil;
import il.ac.jmc.matchday.validation.RegistrationConstraints;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Handles authentication-related routes: login and registration.
 */
@Controller
public class AuthController {

    /** Registration service used to validate and create new users. */
    private final RegistrationService registrationService;

    /**
     * Creates the authentication controller.
     *
     * @param registrationService service used to validate and create new users
     */
    public AuthController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    /**
     * Shows the login page, carrying a safe return URL through the form when given.
     *
     * @param returnUrl optional post-login return URL
     * @param model MVC model populated for the login page
     * @return login view
     */
    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String returnUrl,
                            Model model) {
        if (ReturnUrlUtil.isSafe(returnUrl)) {
            model.addAttribute("returnUrl", returnUrl);
        }
        return "auth/login";
    }

    /**
     * Shows the registration form with an empty command object.
     *
     * @param model MVC model populated for the registration form
     * @return registration view
     */
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("form", new RegistrationForm());
        addConstraints(model);
        return "auth/register";
    }

    /**
     * Validates and creates the account, then redirects to login on success.
     *
     * @param form submitted registration form
     * @param result validation result
     * @param model MVC model reused when validation fails
     * @return registration view on validation errors, otherwise login redirect
     */
    @PostMapping("/register")
    public String registerSubmit(@Valid @ModelAttribute("form") RegistrationForm form,
                                 BindingResult result, Model model) {
        registrationService.validate(form, result);
        if (result.hasErrors()) {
            addConstraints(model);
            return "auth/register";
        }
        registrationService.register(form);
        return "redirect:/login?registered";
    }

    /**
     * Adds registration validation constants used by the template.
     *
     * @param model MVC model to populate
     */
    private void addConstraints(Model model) {
        model.addAttribute("usernameMin", RegistrationConstraints.USERNAME_MIN);
        model.addAttribute("usernameMax", RegistrationConstraints.USERNAME_MAX);
        model.addAttribute("passwordMin", RegistrationConstraints.PASSWORD_MIN);
        model.addAttribute("passwordMax", RegistrationConstraints.PASSWORD_MAX);
        model.addAttribute("emailMax",    RegistrationConstraints.EMAIL_MAX);
    }
}
