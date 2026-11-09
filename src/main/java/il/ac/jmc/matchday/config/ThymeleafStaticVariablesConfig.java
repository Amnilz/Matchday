package il.ac.jmc.matchday.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;

/**
 * Registers app-wide static variables on the Thymeleaf view resolver so every
 * rendered view (including error pages from exception handlers) can use them.
 */
@Configuration
public class ThymeleafStaticVariablesConfig {

    /**
     * Adds global template variables to the configured Thymeleaf view resolver.
     *
     * @param thymeleafViewResolver resolver used by Spring MVC to render Thymeleaf views
     * @param appName application name exposed as {@code appName} in all templates
     */
    public ThymeleafStaticVariablesConfig(ThymeleafViewResolver thymeleafViewResolver,
                                          @Value("${app.name}") String appName) {
        thymeleafViewResolver.addStaticVariable("appName", appName);
    }
}
