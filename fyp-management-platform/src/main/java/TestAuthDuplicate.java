import com.fyp.service.AuthService;

public class TestAuthDuplicate {
    public static void main(String[] args) {
        String email = "testusere8e06@example.com"; // using the same email from previous test
        System.out.println("Testing duplicate signup with email: " + email);
        try {
            String result = AuthService.signup(email, "password123!", "Test User", "STUDENT");
            System.out.println("Result: " + result);
        } catch (Exception e) {
            System.out.println("Exception caught: " + e.getMessage());
        }
    }
}
