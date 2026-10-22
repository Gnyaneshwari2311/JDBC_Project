
package library;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;

public class IssueRepository {

    // Issue a book
    public void issueBook() {

        Scanner sc = new Scanner(System.in);

        System.out.print("Enter book ID: ");
        int bookId = sc.nextInt();

        System.out.print("Enter member ID: ");
        int memberId = sc.nextInt();

        String checkBook =
                "SELECT quantity FROM books WHERE book_id = ?";

        String checkMember =
                "SELECT member_id FROM members WHERE member_id = ?";

        String updateBook =
                "UPDATE books SET quantity = quantity - 1 "
              + "WHERE book_id = ? AND quantity > 0";

        String insertIssue =
                "INSERT INTO book_issues "
              + "(book_id, member_id, issue_date, status, fine) "
              + "VALUES (?, ?, ?, 'ISSUED', 0)";

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                System.out.println("Database connection failed!");
                return;
            }

            con.setAutoCommit(false);

            try {
                // Check whether the book exists and is available
                try (PreparedStatement ps =
                        con.prepareStatement(checkBook)) {

                    ps.setInt(1, bookId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {
                            System.out.println("Book not found!");
                            con.rollback();
                            return;
                        }

                        if (rs.getInt("quantity") <= 0) {
                            System.out.println("Book is not available!");
                            con.rollback();
                            return;
                        }
                    }
                }

                // Check whether the member exists
                try (PreparedStatement ps =
                        con.prepareStatement(checkMember)) {

                    ps.setInt(1, memberId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {
                            System.out.println("Member not found!");
                            con.rollback();
                            return;
                        }
                    }
                }

                // Reduce the available book quantity
                try (PreparedStatement ps =
                        con.prepareStatement(updateBook)) {

                    ps.setInt(1, bookId);

                    if (ps.executeUpdate() == 0) {
                        throw new SQLException(
                                "Book is no longer available.");
                    }
                }

                // Insert the issue record
                try (PreparedStatement ps =
                        con.prepareStatement(insertIssue)) {

                    ps.setInt(1, bookId);
                    ps.setInt(2, memberId);
                    ps.setDate(3, Date.valueOf(LocalDate.now()));

                    ps.executeUpdate();
                }

                con.commit();
                System.out.println("Book issued successfully!");

            } catch (SQLException e) {
                con.rollback();
                System.out.println("Book issue failed!");
                e.printStackTrace();

            } finally {
                con.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.out.println("Database operation failed!");
            e.printStackTrace();
        }
    }

    // Return a book
    public void returnBook() {

        Scanner sc = new Scanner(System.in);

        System.out.print("Enter issue ID: ");
        int issueId = sc.nextInt();

        String findIssue =
                "SELECT book_id, issue_date FROM book_issues "
              + "WHERE issue_id = ? AND status = 'ISSUED'";

        String updateIssue =
                "UPDATE book_issues SET return_date = ?, "
              + "fine = ?, status = 'RETURNED' "
              + "WHERE issue_id = ? AND status = 'ISSUED'";

        String updateBook =
                "UPDATE books SET quantity = quantity + ? "
              + "WHERE book_id = ?";

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                System.out.println("Database connection failed!");
                return;
            }

            con.setAutoCommit(false);

            try {
                int bookId;
                LocalDate issueDate;

                // Find the active issue record
                try (PreparedStatement ps =
                        con.prepareStatement(findIssue)) {

                    ps.setInt(1, issueId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {
                            System.out.println(
                                    "Active issue not found!");
                            con.rollback();
                            return;
                        }

                        bookId = rs.getInt("book_id");
                        issueDate = rs.getDate("issue_date")
                                      .toLocalDate();
                    }
                }

                // Calculate fine: Rs. 5 per day after 14 days
                LocalDate returnDate = LocalDate.now();

                long overdueDays = Math.max(
                        0,
                        ChronoUnit.DAYS.between(
                                issueDate.plusDays(14),
                                returnDate));

                double fine = overdueDays * 5.0;

                // Update the issue record
                try (PreparedStatement ps =
                        con.prepareStatement(updateIssue)) {

                    ps.setDate(1, Date.valueOf(returnDate));
                    ps.setDouble(2, fine);
                    ps.setInt(3, issueId);

                    if (ps.executeUpdate() == 0) {
                        throw new SQLException(
                                "Issue record could not be updated.");
                    }
                }

                // Increase the available book quantity
                try (PreparedStatement ps =
                        con.prepareStatement(updateBook)) {

                    ps.setInt(1, 1);
                    ps.setInt(2, bookId);

                    if (ps.executeUpdate() == 0) {
                        throw new SQLException(
                                "Book record could not be updated.");
                    }
                }

                con.commit();

                System.out.println("Book returned successfully!");
                System.out.println("Overdue days: " + overdueDays);
                System.out.println("Fine: Rs. " + fine);

            } catch (SQLException e) {
                con.rollback();
                System.out.println("Book return failed!");
                e.printStackTrace();

            } finally {
                con.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.out.println("Database operation failed!");
            e.printStackTrace();
        }
    }
}
