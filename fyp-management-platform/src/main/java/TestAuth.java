import com.fyp.service.AuthService;
import java.util.UUID;

public class TestAuth {
    public static void main(String[] args) {
        String email = "testuser" + UUID.randomUUID().toString().substring(0, 5) + "@example.com";
        System.out.println("Testing signup with email: " + email);
        String result = AuthService.signup(email, "password123!", "Test User", "STUDENT");
        System.out.println("Result: " + result);
    }
}
