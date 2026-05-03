import com.fyp.util.SupabaseClient;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DebugSupervisors {
    public static void main(String[] args) {
        try {
            // This is the query used in SupervisorDAO.findAll()
            String query = "/rest/v1/supervisors?select=supervisor_id,employee_id,research_area,slots_available,users(user_id,name,email,is_active,is_email_verified)";
            HttpRequest.Builder req = HttpRequest.newBuilder()
                    .uri(URI.create(SupabaseClient.getBaseUrl() + query))
                    .GET();
            
            HttpResponse<String> res = SupabaseClient.sendRequest(req);
            System.out.println("Code: " + res.statusCode());
            System.out.println("Body: " + res.body());
            
            // Try without is_email_verified
            String query2 = "/rest/v1/supervisors?select=supervisor_id,employee_id,research_area,slots_available,users(user_id,name,email,is_active)";
            HttpRequest.Builder req2 = HttpRequest.newBuilder()
                    .uri(URI.create(SupabaseClient.getBaseUrl() + query2))
                    .GET();
            
            HttpResponse<String> res2 = SupabaseClient.sendRequest(req2);
            System.out.println("Code (Fixed): " + res2.statusCode());
            System.out.println("Body (Fixed): " + res2.body());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
