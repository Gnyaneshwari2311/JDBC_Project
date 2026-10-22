
package library;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    public static Connection getConnection() {

        Connection con = null;

        try {
            String url = "jdbc:mysql://localhost:3306/library_management";
            String username = "root";
            String password = "Verag";

            con = DriverManager.getConnection(url, username, password);

            System.out.println("Database connected successfully!");

        } catch (SQLException e) {
            System.out.println("Database connection failed!");
            e.printStackTrace();
        }

        return con;
    }
}
