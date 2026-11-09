package il.ac.jmc.matchday;

import il.ac.jmc.matchday.init.DataInitializer;
import il.ac.jmc.matchday.init.DemoDataSeeder;
import il.ac.jmc.matchday.repository.TeamRepository;
import il.ac.jmc.matchday.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = MatchdayApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MatchdayApplicationTests {
    @LocalServerPort int port;
    @Autowired TeamRepository teams;
    @Autowired UserRepository users;
    @Autowired DemoDataSeeder seeder;
    @Autowired JdbcTemplate jdbc;
    @Autowired ApplicationContext context;

    @Test
    void seedsFootballDataWithoutImportingAccountsOrActivity() {
        assertThat(teams.count()).isEqualTo(20);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM matches", Long.class)).isPositive();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM match_events", Long.class)).isPositive();
        assertThat(users.count()).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM comments", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM favorite_teams", Long.class)).isZero();
    }

    @Test
    void repeatedStartupDoesNotDuplicateFixtures() {
        long before = teams.count();
        seeder.run(new DefaultApplicationArguments());
        assertThat(teams.count()).isEqualTo(before);
    }

    @Test
    void rendersPublicPagesAndRequiresLoginForPrivateRoutes() throws Exception {
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        for (String path : new String[]{"/", "/leagues", "/teams", "/players", "/matches", "/search", "/login"}) {
            HttpResponse<String> response = client.send(HttpRequest.newBuilder(
                    URI.create("http://127.0.0.1:" + port + path)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as(path).isEqualTo(200);
            assertThat(response.body()).as(path).contains("</html>");
        }
        for (String path : new String[]{"/admin", "/profile"}) {
            HttpResponse<Void> response = client.send(HttpRequest.newBuilder(
                    URI.create("http://127.0.0.1:" + port + path)).GET().build(),
                    HttpResponse.BodyHandlers.discarding());
            assertThat(response.statusCode()).as(path).isEqualTo(302);
            assertThat(response.headers().firstValue("location").orElse("")).endsWith("/login");
        }
    }

    @Test
    void administratorBootstrapIsDisabledUnlessEnabled() {
        assertThat(context.getBeansOfType(DataInitializer.class)).isEmpty();
    }
}
