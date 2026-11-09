package il.ac.jmc.matchday.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables Spring's asynchronous method execution support.
 * Used by {@link il.ac.jmc.matchday.service.WikipediaLogoService}.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Creates the async configuration component.
     */
    public AsyncConfig() {}
}
