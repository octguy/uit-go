package uitgo.driverservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
class DriverServiceApplicationTests {

    @Test
    void contextLoads() {
        // This test ensures the Spring context loads successfully
    }

    @Test
    void applicationStartsSuccessfully() {
        // This test verifies that the application starts without errors
        // If this test passes, it means all beans are properly configured
    }
}
