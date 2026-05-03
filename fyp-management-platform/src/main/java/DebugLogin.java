import com.fyp.util.SupabaseClient;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

public class DebugLogin {
    public static void main(String[] args) {
        String email = "student1@nu.edu.pk";
        String password = "password123!";

        try {
            System.out.println("Debugging login for: " + email);
            JsonObject json = new JsonObject();
            json.addProperty("email", email);
            json.addProperty("password", password);

            HttpRequest.Builder request = HttpRequest.newBuilder()
                    .uri(URI.create(SupabaseClient.getBaseUrl() + "/auth/v1/token?grant_type=password"))
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()));

            HttpResponse<String> response = SupabaseClient.sendRequest(request);
            System.out.println("Auth Token Response Code: " + response.statusCode());
            System.out.println("Auth Token Response Body: " + response.body());

            if (response.statusCode() == 200) {
                JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();
                String token = body.get("access_token").getAsString();
                JsonObject userObj = body.getAsJsonObject("user");
                String userId = userObj.get("id").getAsString();
                System.out.println("Extracted User ID: " + userId);

                HttpRequest.Builder userReq = HttpRequest.newBuilder()
                        .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/users?user_id=eq." + userId))
                        .GET();
                HttpResponse<String> userRes = SupabaseClient.sendAuthenticatedRequest(userReq, token);
                
                System.out.println("User Profile Response Code: " + userRes.statusCode());
                System.out.println("User Profile Response Body: " + userRes.body());

                if (userRes.statusCode() == 200 && !userRes.body().equals("[]")) {
                    System.out.println("SUCCESS! User profile found.");
                } else {
                    System.out.println("FAILED! User profile NOT found in fyp.users.");
                }
            } else {
                System.out.println("FAILED! Invalid email or password at Auth layer.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
