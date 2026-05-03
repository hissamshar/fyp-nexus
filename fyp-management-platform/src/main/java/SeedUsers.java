import com.fyp.service.AuthService;
import java.util.UUID;

public class SeedUsers {
    public static void main(String[] args) {
        System.out.println("Seeding dummy users...");
        String[][] usersToSeed = {
            {"student1@nu.edu.pk", "password123!", "Alice Student", "STUDENT"},
            {"student2@nu.edu.pk", "password123!", "Bob Student", "STUDENT"},
            {"supervisor1@nu.edu.pk", "password123!", "Dr. Carol Supervisor", "SUPERVISOR"},
            {"examiner1@nu.edu.pk", "password123!", "Dr. Dave Examiner", "EXAMINER"},
            {"admin1@nu.edu.pk", "password123!", "Eve Admin", "ADMIN"},
            {"industry1@nu.edu.pk", "password123!", "Frank Industry", "INDUSTRY_PARTNER"}
        };

        for (String[] user : usersToSeed) {
            try {
                System.out.println("Registering: " + user[0] + " as " + user[3]);
                String result = AuthService.signup(user[0], user[1], user[2], user[3]);
                System.out.println("Result: " + result);
                Thread.sleep(1000); // Wait for triggers
            } catch (Exception e) {
                System.out.println("Failed to register " + user[0] + ": " + e.getMessage());
            }
        }
        System.out.println("User seeding complete.");
    }
}
