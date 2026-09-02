package com.ucc.chatbot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.List;

public class SchemaReset implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final Logger log = LoggerFactory.getLogger(SchemaReset.class);

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        Environment env = event.getEnvironment();
        String flag = env.getProperty("DB_RESET_ON_BOOT");
        if (flag == null) flag = System.getenv("DB_RESET_ON_BOOT");
        if (!"true".equalsIgnoreCase(flag)) return;

        String url = env.getProperty("DB_URL");
        if (url == null) url = System.getenv("DB_URL");
        String user = env.getProperty("DB_USERNAME");
        if (user == null) user = System.getenv("DB_USERNAME");
        String pass = env.getProperty("DB_PASSWORD");
        if (pass == null) pass = System.getenv("DB_PASSWORD");
        String driver = env.getProperty("DB_DRIVER");
        if (driver == null) driver = System.getenv("DB_DRIVER");

        if (url == null || user == null || pass == null) {
            log.warn("DB_RESET_ON_BOOT=true but DB connection details missing — skipping");
            return;
        }

        log.warn("DB_RESET_ON_BOOT=true — dropping ALL tables BEFORE Hibernate starts (database will be wiped)...");
        try {
            Class.forName(driver != null ? driver : "org.postgresql.Driver");
            DataSource ds = new DriverManagerDataSource(url, user, pass);
            JdbcTemplate jdbc = new JdbcTemplate(ds);
            List<String> tables = jdbc.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public'", String.class);
            for (String t : tables) {
                try {
                    jdbc.execute("DROP TABLE IF EXISTS \"" + t + "\" CASCADE");
                    log.info("Dropped: {}", t);
                } catch (Exception e) {
                    log.warn("Could not drop {}: {}", t, e.getMessage());
                }
            }
            log.warn("All tables dropped. Hibernate will recreate them on boot.");
        } catch (Exception e) {
            log.error("DB_RESET_ON_BOOT failed: {}", e.getMessage());
        }
    }
}
