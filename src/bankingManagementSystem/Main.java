package bankingManagementSystem;


import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Scanner;

public class Main {

    static Scanner sc = new Scanner(System.in);
    static BankingService bank = new BankingService();

    public static void main(String[] args) {

        while (true) {
            System.out.println("\n===== BANKING MANAGEMENT SYSTEM =====");
            System.out.println("1. Create Account");
            System.out.println("2. Deposit Money");
            System.out.println("3. Withdraw Money");
            System.out.println("4. Fund Transfer");
            System.out.println("5. Transaction History");
            System.out.println("6. Check Balance");
            System.out.println("7. Exit");

            try {
                System.out.print("Enter your choice: ");
                int choice = Integer.parseInt(sc.nextLine().trim());

                switch (choice) {

                    case 1:
                        System.out.print("Enter customer name: ");
                        String name = sc.nextLine().trim();

                        System.out.print("Enter email: ");
                        String email = sc.nextLine().trim();

                        System.out.print("Enter phone number: ");
                        String phone = sc.nextLine().trim();

                        System.out.print("Enter opening balance: ");
                        BigDecimal openingBalance =
                                new BigDecimal(sc.nextLine().trim());

                        int newAccountId = bank.createAccount(
                                name, email, phone, openingBalance);

                        System.out.println("Account created successfully!");
                        System.out.println("Account ID: " + newAccountId);
                        bank.checkBalance(newAccountId);
                        break;

                    case 2:
                        System.out.print("Enter account ID: ");
                        int depositId =
                                Integer.parseInt(sc.nextLine().trim());

                        System.out.print("Enter deposit amount: ");
                        BigDecimal depositAmount =
                                new BigDecimal(sc.nextLine().trim());

                        bank.deposit(depositId, depositAmount);
                        System.out.println("Deposit successful!");
                        bank.checkBalance(depositId);
                        break;

                    case 3:
                        System.out.print("Enter account ID: ");
                        int withdrawId =
                                Integer.parseInt(sc.nextLine().trim());

                        System.out.print("Enter withdrawal amount: ");
                        BigDecimal withdrawAmount =
                                new BigDecimal(sc.nextLine().trim());

                        bank.withdraw(withdrawId, withdrawAmount);
                        System.out.println("Withdrawal successful!");
                        bank.checkBalance(withdrawId);
                        break;

                    case 4:
                        System.out.print("Enter sender account ID: ");
                        int senderId =
                                Integer.parseInt(sc.nextLine().trim());

                        System.out.print("Enter receiver account ID: ");
                        int receiverId =
                                Integer.parseInt(sc.nextLine().trim());

                        System.out.print("Enter transfer amount: ");
                        BigDecimal transferAmount =
                                new BigDecimal(sc.nextLine().trim());

                        bank.transfer(senderId, receiverId, transferAmount);

                        System.out.println("Transfer successful!");

                        System.out.println("\nSender account:");
                        bank.checkBalance(senderId);

                        System.out.println("\nReceiver account:");
                        bank.checkBalance(receiverId);
                        break;

                    case 5:
                        System.out.print("Enter account ID: ");
                        int historyId =
                                Integer.parseInt(sc.nextLine().trim());

                        bank.showTransactionHistory(historyId);
                        break;

                    case 6:
                        System.out.print("Enter account ID: ");
                        int balanceId =
                                Integer.parseInt(sc.nextLine().trim());

                        bank.checkBalance(balanceId);
                        break;

                    case 7:
                        System.out.println("Thank you for using our bank.");
                        sc.close();
                        return;

                    default:
                        System.out.println("Please select a number from 1 to 7.");
                }

            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");

            } catch (IllegalArgumentException e) {
                System.out.println("Invalid input: " + e.getMessage());

            } catch (SQLException e) {
                System.out.println("Operation failed: " + e.getMessage());

            } catch (Exception e) {
                System.out.println("Something went wrong: " + e.getMessage());
            }
        }
    }
}