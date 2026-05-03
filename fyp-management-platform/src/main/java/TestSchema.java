import com.fyp.util.SupabaseClient;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TestSchema {
    public static void main(String[] args) {
        try {
            HttpRequest.Builder req = HttpRequest.newBuilder()
                    .uri(URI.create(SupabaseClient.getBaseUrl() + "/rest/v1/users?select=*&limit=1"))
                    .GET();
            
            HttpResponse<String> res = SupabaseClient.sendRequest(req);
            System.out.println("Code: " + res.statusCode());
            System.out.println("Body: " + res.body());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
