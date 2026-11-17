package br.com.curralzap.backend.shared.infrastructure;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Converts Railway/Heroku DATABASE_URL (postgres://user:pass@host:port/db)
 * into the JDBC properties the app expects, so no manual URL construction
 * is needed in the Railway dashboard.
 */
public class DatabaseUrlInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }

        URI uri = URI.create(databaseUrl.replace("postgres://", "postgresql://"));

        String host = uri.getHost();
        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        String path = uri.getPath();
        String userInfo = uri.getUserInfo();

        String username = userInfo;
        String password = "";
        if (userInfo != null && userInfo.contains(":")) {
            int sep = userInfo.indexOf(':');
            username = userInfo.substring(0, sep);
            password = userInfo.substring(sep + 1);
        }

        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;

        Map<String, Object> props = new HashMap<>();
        props.put("DB_URL", jdbcUrl);
        props.put("DB_USERNAME", username);
        props.put("DB_PASSWORD", password);
        props.put("DB_DRIVER_CLASS_NAME", "org.postgresql.Driver");

        context.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource("railway-database-url", props));
    }
}
