import com.fyp.service.AuthService;

public class TestRegistrationFlow {
    public static void main(String[] args) {
        String email = "freshuser123@example.com";
        System.out.println("Testing signup with fresh email: " + email);
        try {
            String result = AuthService.signup(email, "password123!", "Fresh User", "STUDENT");
            System.out.println("Result returned to Controller: " + result);
            
            if ("AUTO_LOGGED_IN".equals(result)) {
                System.out.println("Successfully bypassed OTP and auto-logged in!");
                // Simulating Main.loadView("MainDashboardView") success
            } else if (result != null) {
                System.out.println("Returned user ID: " + result + ". Showing OTP view.");
            } else {
                System.out.println("Result was null. This should not happen anymore.");
            }
        } catch (Exception e) {
            System.out.println("Task failed with Exception: " + e.getMessage());
        }
    }
}
