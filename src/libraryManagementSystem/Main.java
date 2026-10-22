package libraryManagementSystem;
import java.sql.*;
import java.util.Scanner;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Main {
static final String URL ="jdbc:mysql://localhost:3306/library_db";
    static final String USER = "root";
    static final String PASSWORD = "Verag";

    static Scanner sc = new Scanner(System.in);

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void main(String[] args) {

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL JDBC driver not found!");
            return;
        }

        while (true) {
            System.out.println("\n===== LIBRARY MANAGEMENT SYSTEM =====");
            System.out.println("1. Add Book");
            System.out.println("2. View All Books");
            System.out.println("3. Search Book");
            System.out.println("4. Register Member");
            System.out.println("5. View All Members");
            System.out.println("6. Issue Book");
            System.out.println("7. Return Book");
            System.out.println("8. Calculate Fine");
            System.out.println("9. View Issue History");
            System.out.println("10. Exit");
            System.out.print("Enter your choice: ");

            int choice = readInt();

            try {
                switch (choice) {
                    case 1:
                        addBook();
                        break;
                    case 2:
                        viewBooks();
                        break;
                    case 3:
                        searchBook();
                        break;
                    case 4:
                        registerMember();
                        break;
                    case 5:
                        viewMembers();
                        break;
                    case 6:
                        issueBook();
                        break;
                    case 7:
                        returnBook();
                        break;
                    case 8:
                        calculateFine();
                        break;
                    case 9:
                        viewIssueHistory();
                        break;
                    case 10:
                        System.out.println("Thank you!");
                        sc.close();
                        return;
                    default:
                        System.out.println("Invalid choice!");
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
    }

    static int readInt() {
        while (!sc.hasNextInt()) {
            System.out.print("Enter a valid number: ");
            sc.next();
        }
        int value = sc.nextInt();
        sc.nextLine();
        return value;
    }

    // 1. Add Book
    static void addBook() throws SQLException {
        System.out.print("Enter book title: ");
        String title = sc.nextLine();

        System.out.print("Enter author name: ");
        String author = sc.nextLine();

        System.out.print("Enter quantity: ");
        int quantity = readInt();

        if (title.trim().isEmpty() || author.trim().isEmpty()
                || quantity <= 0) {
            System.out.println("Enter valid book details.");
            return;
        }

        String sql = "INSERT INTO books " + "(title, author, quantity, available_quantity) " + "VALUES (?, ?, ?, ?)";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, title);
            ps.setString(2, author);
            ps.setInt(3, quantity);
            ps.setInt(4, quantity);

            ps.executeUpdate();
            System.out.println("Book added successfully!");
        }
    }

    // 2. View All Books
    static void viewBooks() throws SQLException {
        String sql = "SELECT * FROM books ORDER BY book_id";

        try (Connection con = getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\n--- BOOK LIST ---");

            while (rs.next()) {
                System.out.println(
                    "ID: " + rs.getInt("book_id")
                    + " | Title: " + rs.getString("title")
                    + " | Author: " + rs.getString("author")
                    + " | Total: " + rs.getInt("quantity")
                    + " | Available: "
                    + rs.getInt("available_quantity")
                );
            }
        }
    }

    // 3. Search Book
    static void searchBook() throws SQLException {
        System.out.print("Enter book ID or title: ");
        String input = sc.nextLine();

        String sql = "SELECT * FROM books "
                + "WHERE book_id = ? OR title LIKE ?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            int id = -1;
            try {
                id = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                // Search by title when input is not a number.
            }

            ps.setInt(1, id);
            ps.setString(2, "%" + input + "%");

            try (ResultSet rs = ps.executeQuery()) {
                boolean found = false;

                while (rs.next()) {
                    found = true;
                    System.out.println(
                        "ID: " + rs.getInt("book_id")
                        + " | Title: " + rs.getString("title")
                        + " | Author: " + rs.getString("author")
                        + " | Available: "
                        + rs.getInt("available_quantity")
                    );
                }

                if (!found) {
                    System.out.println("Book not found.");
                }
            }
        }
    }

    // 4. Register Member
    static void registerMember() throws SQLException {
        System.out.print("Enter member name: ");
        String name = sc.nextLine();

        System.out.print("Enter email: ");
        String email = sc.nextLine();

        System.out.print("Enter phone: ");
        String phone = sc.nextLine();

        if (name.trim().isEmpty() || email.trim().isEmpty()) {
            System.out.println("Name and email are required.");
            return;
        }

        String sql = "INSERT INTO members (name, email, phone) "
                + "VALUES (?, ?, ?)";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, phone);

            ps.executeUpdate();
            System.out.println("Member registered successfully!");
        }
    }

    // 5. View All Members
    static void viewMembers() throws SQLException {
        String sql = "SELECT * FROM members ORDER BY member_id";

        try (Connection con = getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\n--- MEMBER LIST ---");

            while (rs.next()) {
                System.out.println(
                    "ID: " + rs.getInt("member_id")
                    + " | Name: " + rs.getString("name")
                    + " | Email: " + rs.getString("email")
                    + " | Phone: " + rs.getString("phone")
                );
            }
        }
    }

    // 6. Issue Book (transaction)
    static void issueBook() throws SQLException {
        System.out.print("Enter book ID: ");
        int bookId = readInt();

        System.out.print("Enter member ID: ");
        int memberId = readInt();

        try (Connection con = getConnection()) {
            con.setAutoCommit(false);

            try {
                // Lock the book row to prevent overselling copies.
                String bookSql = "SELECT available_quantity FROM books "
                        + "WHERE book_id = ? FOR UPDATE";

                int available;

                try (PreparedStatement ps =
                        con.prepareStatement(bookSql)) {
                    ps.setInt(1, bookId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Book not found.");
                        }
                        available = rs.getInt("available_quantity");
                    }
                }

                if (available <= 0) {
                    throw new SQLException("No copies available.");
                }

                String memberSql =
                        "SELECT member_id FROM members WHERE member_id = ?";

                try (PreparedStatement ps =
                        con.prepareStatement(memberSql)) {
                    ps.setInt(1, memberId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Member not found.");
                        }
                    }
                }

                LocalDate issueDate = LocalDate.now();
                LocalDate dueDate = issueDate.plusDays(7);

                String issueSql = "INSERT INTO book_issues "
                        + "(book_id, member_id, issue_date, due_date) "
                        + "VALUES (?, ?, ?, ?)";

                try (PreparedStatement ps =
                        con.prepareStatement(issueSql)) {
                    ps.setInt(1, bookId);
                    ps.setInt(2, memberId);
                    ps.setDate(3, Date.valueOf(issueDate));
                    ps.setDate(4, Date.valueOf(dueDate));
                    ps.executeUpdate();
                }

                String updateSql = "UPDATE books "
                        + "SET available_quantity = available_quantity - 1 "
                        + "WHERE book_id = ?";

                try (PreparedStatement ps =
                        con.prepareStatement(updateSql)) {
                    ps.setInt(1, bookId);
                    ps.executeUpdate();
                }

                con.commit();

                System.out.println("Book issued successfully!");
                System.out.println("Issue Date: " + issueDate);
                System.out.println("Due Date: " + dueDate);

            } catch (SQLException e) {
                con.rollback();
                System.out.println(
                    "Issue failed. Changes rolled back: " + e.getMessage()
                );
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    // 7. Return Book (transaction)
    static void returnBook() throws SQLException {
        System.out.print("Enter issue ID: ");
        int issueId = readInt();

        try (Connection con = getConnection()) {
            con.setAutoCommit(false);

            try {
                String sql = "SELECT book_id, due_date FROM book_issues "
                        + "WHERE issue_id = ? AND return_date IS NULL "
                        + "FOR UPDATE";

                int bookId;
                LocalDate dueDate;

                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, issueId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                "Issue not found or book already returned."
                            );
                        }

                        bookId = rs.getInt("book_id");
                        dueDate = rs.getDate("due_date").toLocalDate();
                    }
                }

                LocalDate returnDate = LocalDate.now();

                long lateDays = Math.max(
                    0, ChronoUnit.DAYS.between(dueDate, returnDate)
                );

                double fine = lateDays * 5.0;

                String updateIssue = "UPDATE book_issues "
                        + "SET return_date = ?, fine_amount = ? "
                        + "WHERE issue_id = ? AND return_date IS NULL";

                try (PreparedStatement ps =
                        con.prepareStatement(updateIssue)) {
                    ps.setDate(1, Date.valueOf(returnDate));
                    ps.setDouble(2, fine);
                    ps.setInt(3, issueId);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException(
                            "Return failed. Please try again."
                        );
                    }
                }

                String updateBook = "UPDATE books "
                        + "SET available_quantity = available_quantity + 1 "
                        + "WHERE book_id = ?";

                try (PreparedStatement ps =
                        con.prepareStatement(updateBook)) {
                    ps.setInt(1, bookId);
                    ps.executeUpdate();
                }

                con.commit();

                System.out.println("Book returned successfully!");
                System.out.println("Late Days: " + lateDays);
                System.out.printf("Fine Amount: Rs. %.2f%n", fine);

            } catch (SQLException e) {
                con.rollback();
                System.out.println(
                    "Return failed. Changes rolled back: " + e.getMessage()
                );
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    // 8. Calculate Fine
    static void calculateFine() throws SQLException {
        System.out.print("Enter issue ID: ");
        int issueId = readInt();

        String sql = "SELECT due_date, return_date, fine_amount "
                + "FROM book_issues WHERE issue_id = ?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, issueId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Issue record not found.");
                    return;
                }

                LocalDate dueDate =
                        rs.getDate("due_date").toLocalDate();

                Date returned = rs.getDate("return_date");
                LocalDate checkDate = returned == null
                        ? LocalDate.now()
                        : returned.toLocalDate();

                long lateDays = Math.max(
                    0, ChronoUnit.DAYS.between(dueDate, checkDate)
                );

                double fine = returned == null
                        ? lateDays * 5.0
                        : rs.getDouble("fine_amount");

                System.out.println("Due Date: " + dueDate);
                System.out.println(
                    returned == null
                        ? "Status: Book not yet returned"
                        : "Return Date: " + returned
                );
                System.out.println("Late Days: " + lateDays);
                System.out.printf("Fine Amount: Rs. %.2f%n", fine);
            }
        }
    }

    // 9. View Issue History using JOIN
    static void viewIssueHistory() throws SQLException {
        String sql = "SELECT bi.issue_id, b.title, m.name, "
                + "bi.issue_date, bi.due_date, bi.return_date, "
                + "bi.fine_amount "
                + "FROM book_issues bi "
                + "JOIN books b ON bi.book_id = b.book_id "
                + "JOIN members m ON bi.member_id = m.member_id "
                + "ORDER BY bi.issue_id DESC";

        try (Connection con = getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\n--- ISSUE HISTORY ---");

            while (rs.next()) {
                System.out.println(
                    "Issue ID: " + rs.getInt("issue_id")
                    + " | Book: " + rs.getString("title")
                    + " | Member: " + rs.getString("name")
                    + " | Issued: " + rs.getDate("issue_date")
                    + " | Due: " + rs.getDate("due_date")
                    + " | Returned: " + rs.getDate("return_date")
                    + " | Fine: Rs. " + rs.getBigDecimal("fine_amount")
                );
            }
        }
    }
}