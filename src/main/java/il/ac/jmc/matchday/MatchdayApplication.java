package il.ac.jmc.matchday;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the Matchday football league manager.
 */
@SpringBootApplication
public class MatchdayApplication {

	/**
	 * Creates the Spring Boot application marker class.
	 */
	public MatchdayApplication() {}

	/**
	 * Boots the application.
	 *
	 * @param args command-line arguments passed to Spring Boot
	 */
	public static void main(String[] args) {
		SpringApplication.run(MatchdayApplication.class, args);
	}

}
