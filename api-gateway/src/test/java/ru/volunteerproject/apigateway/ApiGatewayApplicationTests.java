package ru.volunteerproject.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "security.jwt.secret=S9YodvYv+T0py+pQpdo6KUEY7OFt0nFoxG0i5YYW9N4="
})
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }

}
