import java.sql.Connection;
import java.sql.DriverManager;

public class TestConn {
    public static void main(String[] args) {
        String[] urls = {
            "jdbc:postgresql://db.mwoqposmpqftonmzfnsj.supabase.co:6543/postgres?sslmode=require&user=postgres&password=BJT05ic8nNC13ECr",
            "jdbc:postgresql://db.mwoqposmpqftonmzfnsj.supabase.co:6543/postgres?sslmode=require&user=postgres.mwoqposmpqftonmzfnsj&password=BJT05ic8nNC13ECr",
            "jdbc:postgresql://db.mwoqposmpqftonmzfnsj.supabase.co:5432/postgres?sslmode=require&user=postgres&password=BJT05ic8nNC13ECr"
        };
        for (int i = 0; i < urls.length; i++) {
            try {
                Connection c = DriverManager.getConnection(urls[i]);
                System.out.println("Success " + i);
                c.close();
            } catch (Exception e) {
                System.out.println("Failed " + i + ": " + e.getMessage());
            }
        }
    }
}
