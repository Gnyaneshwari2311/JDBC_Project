
package library;

import java.util.Scanner;

public class main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        BookRepository bookRepository = new BookRepository();
        IssueRepository issueRepository = new IssueRepository();

        int choice;

        do {
            System.out.println("\n===== LIBRARY MANAGEMENT SYSTEM =====");
            System.out.println("1. Add Book");
            System.out.println("2. Search Book");
            System.out.println("3. Issue Book");
            System.out.println("4. Return Book");
            System.out.println("5. Exit");
            System.out.print("Enter your choice: ");

            choice = sc.nextInt();

            switch (choice) {

                case 1:
                    bookRepository.addBook();
                    break;

                case 2:
                    bookRepository.searchBook();
                    break;

                case 3:
                    issueRepository.issueBook();
                    break;

                case 4:
                    issueRepository.returnBook();
                    break;

                case 5:
                    System.out.println(
                            "Thank you for using the Library Management System!");
                    break;

                default:
                    System.out.println("Invalid choice! Try again.");
            }

        } while (choice != 5);

        sc.close();
    }
}
