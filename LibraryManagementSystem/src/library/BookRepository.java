
package library;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class BookRepository {

    // Add a new book
    public void addBook() {

        Scanner sc = new Scanner(System.in);

        System.out.print("Enter book title: ");
        String title = sc.nextLine();

        System.out.print("Enter author name: ");
        String author = sc.nextLine();

        System.out.print("Enter book quantity: ");
        int quantity = sc.nextInt();

        String sql = "INSERT INTO books (title, author, quantity) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (con == null) {
                System.out.println("Database connection failed!");
                return;
            }

            ps.setString(1, title);
            ps.setString(2, author);
            ps.setInt(3, quantity);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Book added successfully!");
            }

        } catch (SQLException e) {
            System.out.println("Failed to add book!");
            e.printStackTrace();
        }
    }

    // Search books by title
    public void searchBook() {

        Scanner sc = new Scanner(System.in);

        System.out.print("Enter book title to search: ");
        String title = sc.nextLine();

        String sql = "SELECT * FROM books WHERE title LIKE ?";

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                System.out.println("Database connection failed!");
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, "%" + title + "%");

                try (ResultSet rs = ps.executeQuery()) {

                    boolean found = false;

                    while (rs.next()) {
                        found = true;

                        System.out.println("\nBook ID: "
                                + rs.getInt("book_id"));

                        System.out.println("Title: "
                                + rs.getString("title"));

                        System.out.println("Author: "
                                + rs.getString("author"));

                        System.out.println("Quantity: "
                                + rs.getInt("quantity"));

                        System.out.println("----------------------");
                    }

                    if (!found) {
                        System.out.println("Book not found!");
                    }
                }
            }

        } catch (SQLException e) {
            System.out.println("Search failed!");
            e.printStackTrace();
        }
    }
}

