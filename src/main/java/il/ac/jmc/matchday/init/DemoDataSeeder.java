package il.ac.jmc.matchday.init;

import il.ac.jmc.matchday.repository.TeamRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;

/** Optional football fixtures for a fresh local database; never creates user accounts. */
@Component
@Order(10)
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {
    private final TeamRepository teamRepository;
    private final DataSource dataSource;

    public DemoDataSeeder(TeamRepository teamRepository, DataSource dataSource) {
        this.teamRepository = teamRepository;
        this.dataSource = dataSource;
    }

    /** Seeds once, preserving referential integrity and rolling back failed imports. */
    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) {
        if (teamRepository.count() > 0) return;
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("demo-data.sql"));
        populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
        populator.execute(dataSource);
    }
}
