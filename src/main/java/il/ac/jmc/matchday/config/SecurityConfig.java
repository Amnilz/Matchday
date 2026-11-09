package il.ac.jmc.matchday.config;

import il.ac.jmc.matchday.security.ReturnUrlSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static il.ac.jmc.matchday.config.Roles.ADMIN;

/**
 * Spring Security configuration for the Matchday application.
 * Defines access rules, the custom login page, and the password encoder bean.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Login success handler that honors safe return URLs. */
    private final ReturnUrlSuccessHandler returnUrlSuccessHandler;

    /**
     * Creates the security configuration with the app's authentication collaborators.
     *
     * @param returnUrlSuccessHandler login success handler that honors safe return URLs
     */
    public SecurityConfig(ReturnUrlSuccessHandler returnUrlSuccessHandler) {
        this.returnUrlSuccessHandler = returnUrlSuccessHandler;
    }

    /**
     * Configures HTTP security: public routes, admin-only routes, form login, and logout.
     *
     * @param http the {@link HttpSecurity} builder provided by Spring Security
     * @return the built {@link SecurityFilterChain}
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/", "/leagues/**", "/teams/**", "/players/**", "/matches/**", "/league-table", "/search/**").permitAll()
                .requestMatchers("/login", "/register").permitAll()
                .requestMatchers("/admin/**").hasAuthority(ADMIN)
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(returnUrlSuccessHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .permitAll()
            );
        return http.build();
    }

    /**
     * Returns a BCrypt password encoder bean used throughout the application.
     *
     * @return BCrypt password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
