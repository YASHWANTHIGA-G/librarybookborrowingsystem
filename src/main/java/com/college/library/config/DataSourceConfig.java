package com.college.library.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${spring.datasource.url}")
    private String mysqlUrl;

    @Value("${spring.datasource.username}")
    private String mysqlUser;

    @Value("${spring.datasource.password}")
    private String mysqlPassword;

    public static String activeDatabaseType = "UNKNOWN";

    @Bean
    @Primary
    public DataSource dataSource() {
        log.info("Testing connection to MySQL at: {}", mysqlUrl);

        boolean mysqlAvailable = false;
        try {
            // Test connection directly before initializing Hikari pool
            DriverManager.setLoginTimeout(3);
            try (Connection conn = DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPassword)) {
                if (conn.isValid(2)) {
                    mysqlAvailable = true;
                    log.info("SUCCESS: Connected to MySQL database on localhost:3306!");
                }
            }
        } catch (Exception ex) {
            log.warn("MySQL connection test failed: {}", ex.getMessage());
            log.warn("Tip: Set environment variable SPRING_DATASOURCE_PASSWORD if your local MySQL has a custom password.");
        }

        if (mysqlAvailable) {
            activeDatabaseType = "MySQL 8.0";
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(mysqlUrl);
            config.setUsername(mysqlUser);
            config.setPassword(mysqlPassword);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setPoolName("MySQLLibraryPool");
            return new HikariDataSource(config);
        } else {
            log.info("==================================================================");
            log.info("ACTIVATING IN-MEMORY DATABASE FALLBACK FOR FLAWLESS DEMO EXECUTION");
            log.info("To switch to MySQL, set SPRING_DATASOURCE_PASSWORD in environment.");
            log.info("==================================================================");
            activeDatabaseType = "H2 Standalone (MySQL credentials pending)";

            HikariConfig h2Config = new HikariConfig();
            h2Config.setJdbcUrl("jdbc:h2:mem:library_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL");
            h2Config.setDriverClassName("org.h2.Driver");
            h2Config.setUsername("sa");
            h2Config.setPassword("");
            h2Config.setMaximumPoolSize(10);
            h2Config.setPoolName("H2FallbackLibraryPool");
            return new HikariDataSource(h2Config);
        }
    }
}
