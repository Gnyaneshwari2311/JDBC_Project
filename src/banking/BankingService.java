package banking;



import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

public class BankingService {

    // Validate money amounts.
    private void checkAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 || amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "Amount must be positive with at most 2 decimal places.");
        }
    }

    // Get an account balance and lock the account during a transaction.
    private BigDecimal getBalanceForUpdate(Connection con, int accountId)
            throws SQLException {

        String sql =
                "SELECT balance FROM accounts WHERE account_id = ? FOR UPDATE";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, accountId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("balance");
                }

                throw new SQLException(
                        "Account ID " + accountId + " not found.");
            }
        }
    }

    // Update the balance of an account.
    private void updateBalance(Connection con, int accountId,
                               BigDecimal balance) throws SQLException {

        String sql =
                "UPDATE accounts SET balance = ? WHERE account_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, balance);
            ps.setInt(2, accountId);

            if (ps.executeUpdate() != 1) {
                throw new SQLException("Balance update failed.");
            }
        }
    }

    // Save a transaction in the database.
    private void addTransaction(Connection con, int accountId,
                                Integer relatedAccountId, String type,
                                BigDecimal amount, String description)
            throws SQLException {

        String sql =
                "INSERT INTO transactions "
                + "(account_id, related_account_id, transaction_type, "
                + "amount, description) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, accountId);

            if (relatedAccountId == null) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(2, relatedAccountId);
            }

            ps.setString(3, type);
            ps.setBigDecimal(4, amount);
            ps.setString(5, description);

            ps.executeUpdate();
        }
    }

    // 1. Create a customer and bank account.
    public int createAccount(String name, String email, String phone,
                             BigDecimal openingBalance) throws SQLException {

        if (name == null || name.trim().isEmpty()
                || phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Name and phone number are required.");
        }

        if (openingBalance == null || openingBalance.compareTo(BigDecimal.ZERO) < 0 || openingBalance.scale() > 2) {
            throw new IllegalArgumentException(
                    "Opening balance must be non-negative with at most 2 decimals.");
        }

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                String customerSql =
                        "INSERT INTO customers (full_name, email, phone) "
                        + "VALUES (?, ?, ?)";

                int customerId;

                try (PreparedStatement ps = con.prepareStatement(
                        customerSql, Statement.RETURN_GENERATED_KEYS)) {

                    ps.setString(1, name.trim());

                    if (email == null || email.trim().isEmpty()) {
                        ps.setNull(2, Types.VARCHAR);
                    } else {
                        ps.setString(2, email.trim());
                    }

                    ps.setString(3, phone.trim());
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "Customer creation failed.");
                        }

                        customerId = rs.getInt(1);
                    }
                }

                String accountSql =
                        "INSERT INTO accounts (customer_id, balance) "
                        + "VALUES (?, ?)";

                int accountId;

                try (PreparedStatement ps = con.prepareStatement(
                        accountSql, Statement.RETURN_GENERATED_KEYS)) {

                    ps.setInt(1, customerId);
                    ps.setBigDecimal(2, openingBalance);
                    ps.executeUpdate();

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "Account creation failed.");
                        }

                        accountId = rs.getInt(1);
                    }
                }

                if (openingBalance.compareTo(BigDecimal.ZERO) > 0) {
                    addTransaction(con, accountId, null,
                            "DEPOSIT", openingBalance, "Opening balance");
                }

                con.commit();
                return accountId;

            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // 2. Deposit money.
    public void deposit(int accountId, BigDecimal amount)
            throws SQLException {

        checkAmount(amount);

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                BigDecimal balance =
                        getBalanceForUpdate(con, accountId);

                updateBalance(con, accountId, balance.add(amount));

                addTransaction(con, accountId, null,
                        "DEPOSIT", amount, "Money deposited");

                con.commit();

            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // 3. Withdraw money.
    public void withdraw(int accountId, BigDecimal amount)
            throws SQLException {

        checkAmount(amount);

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                BigDecimal balance =
                        getBalanceForUpdate(con, accountId);

                if (balance.compareTo(amount) < 0) {
                    throw new SQLException("Insufficient balance.");
                }

                updateBalance(con, accountId, balance.subtract(amount));

                addTransaction(con, accountId, null,
                        "WITHDRAWAL", amount, "Money withdrawn");

                con.commit();

            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // 4. Transfer money between two accounts.
    public void transfer(int senderId, int receiverId, BigDecimal amount)
            throws SQLException {

        checkAmount(amount);

        if (senderId == receiverId) {
            throw new IllegalArgumentException(
                    "Sender and receiver must be different accounts.");
        }

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                // Lock both accounts in a consistent order.
                int firstId = Math.min(senderId, receiverId);
                int secondId = Math.max(senderId, receiverId);

                BigDecimal firstBalance =
                        getBalanceForUpdate(con, firstId);

                BigDecimal secondBalance =
                        getBalanceForUpdate(con, secondId);

                BigDecimal senderBalance =
                        senderId == firstId
                                ? firstBalance : secondBalance;

                BigDecimal receiverBalance =
                        receiverId == firstId
                                ? firstBalance : secondBalance;

                if (senderBalance.compareTo(amount) < 0) {
                    throw new SQLException("Insufficient balance.");
                }

                // Deduct from sender and add to receiver.
                updateBalance(con, senderId,
                        senderBalance.subtract(amount));

                updateBalance(con, receiverId,
                        receiverBalance.add(amount));

                // Save both sides of the transfer.
                addTransaction(con, senderId, receiverId,
                        "TRANSFER_OUT", amount,
                        "Transfer to account " + receiverId);

                addTransaction(con, receiverId, senderId,
                        "TRANSFER_IN", amount,
                        "Transfer from account " + senderId);

                // Save all changes together.
                con.commit();

            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // 5. Display transaction history.
    public void showTransactionHistory(int accountId)
            throws SQLException {

        String sql =
                "SELECT transaction_id, transaction_type, amount, "
                + "description, created_at FROM transactions "
                + "WHERE account_id = ? "
                + "ORDER BY created_at DESC, transaction_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, accountId);

            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("\n========== TRANSACTION HISTORY ==========");

                System.out.printf("%-6s %-14s %-14s %-22s%n",
                        "ID", "TYPE", "AMOUNT", "DATE");

                boolean found = false;

                while (rs.next()) {
                    found = true;

                    System.out.printf("%-6d %-14s Rs. %-10s %-22s%n",
                            rs.getLong("transaction_id"),
                            rs.getString("transaction_type"),
                            rs.getBigDecimal("amount"),
                            rs.getTimestamp("created_at"));

                    System.out.println("Details: "
                            + rs.getString("description"));
                }

                if (!found) {
                    System.out.println("No transactions found.");
                }

                System.out.println("=========================================");
            }
        }
    }

    // 6. Display current balance.
    public void checkBalance(int accountId) throws SQLException {

        String sql =
                "SELECT balance FROM accounts WHERE account_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, accountId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("\n======= ACCOUNT BALANCE =======");
                    System.out.println("Account ID: " + accountId);
                    System.out.printf("Available Balance: Rs. %.2f%n",
                            rs.getBigDecimal("balance"));
                    System.out.println("===============================");
                } else {
                    System.out.println("Account not found.");
                }
            }
        }
    }
}