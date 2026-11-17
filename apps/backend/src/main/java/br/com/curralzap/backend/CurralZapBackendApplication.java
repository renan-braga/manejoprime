package br.com.curralzap.backend;

import br.com.curralzap.backend.shared.infrastructure.DatabaseUrlInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CurralZapBackendApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(CurralZapBackendApplication.class);
        app.addInitializers(new DatabaseUrlInitializer());
        app.run(args);
    }
}
