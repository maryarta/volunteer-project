import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.session.SessionProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SessionProperties.class)
public class AuthServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
