package banking;

import org.sqlite.SQLiteDataSource;
import java.sql.*;
import java.util.Random;
import java.util.Scanner;

public class Main {
    static Random random = new Random();
    static Scanner scanner = new Scanner(System.in);

    public static String generateCardNum() {
        StringBuilder builder = new StringBuilder("400000");
        for (int i = 0; i < 9; i++) {
            builder.append(random.nextInt(10));
        }

        //Luhn Algoritm
        int sum = 0;
        for (int i = 0; i < builder.length(); i++) {
            int c = builder.charAt(i) - '0';

            if (i % 2 == 0) {
                c = c < 5 ? c * 2 : c * 2 - 9;
            }
            sum += c;
        }
        builder.append((10 - (sum % 10)) % 10);
        return builder.toString();
    }

    public static boolean checkCardLuhn(String cardNumber) {
        int checkSum = cardNumber.charAt(cardNumber.length() - 1);
        String temp = cardNumber.substring(0, cardNumber.length() - 1);
        int total = 0;
        for (int i = 0; i < temp.length(); i++) {
            int c = temp.charAt(i) - '0';

            if (i % 2 == 0) {
                c = c < 5 ? c * 2 : c * 2 - 9;
            }
            total += c;
        }
        return checkSum == (10 - (total % 10)) % 10;
    }

    public static String createPin() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            builder.append(random.nextInt(10)); // generate random pin-code
        }
        return builder.toString();
    }

    public static void mainMenu() {

        System.out.println("1. Create an account\n2. Log into account\n0. Exit");
        switch (scanner.nextInt()) {
            case 1:
                generateNewCard();
                break;
            case 2:
                checkCard();
                break;
            case 0:
                exit();
                break;
            default:
                break;
        }
    }

    public static void generateNewCard() {

        String tempCard = generateCardNum();
        String tempPin = createPin();
        String sql = "INSERT INTO card (number, pin) VALUES (?, ?)";

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:card.s3db");
        try (Connection con = dataSource.getConnection()) {
            try (Statement statement = con.createStatement()) {

                PreparedStatement pstmt = con.prepareStatement(sql);
                pstmt.setString(1, tempCard);
                pstmt.setString(2, tempPin);

                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("\nYour card have been created\n" +
                "Your card number:\n" + tempCard + "\nYour card PIN:\n" + tempPin + "\n");

        mainMenu();
    }

    public static void checkCard() {

        System.out.println("\nEnter your card number:");
        String checkCard = scanner.next();
        System.out.println("\nEnter your PIN:");
        String checkPin = scanner.next();

        String sql = "SELECT pin FROM card WHERE number = ?";
        String pin = "";

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:card.s3db");

        try (Connection con = dataSource.getConnection()) {
            try (Statement statement = con.createStatement()) {

                PreparedStatement pstmt = con.prepareStatement(sql);
                pstmt.setString(1, checkCard);

                try (ResultSet cardCheck = pstmt.executeQuery()) {
                    while (cardCheck.next()) {

                        pin = cardCheck.getString("pin");

                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (checkPin.equals(pin)) {
            System.out.println("You have successfully logged in!\n");
            Account(checkCard);
        } else {
            System.out.println("\nWrong card number or PIN!\n");
            mainMenu();
        }
    }

    public static void Account(String cardNumber) {
        String sql;
        long balance = 0;

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:card.s3db");

        try (Connection con = dataSource.getConnection()) {
            try (Statement statement = con.createStatement()) {

                sql = "SELECT balance FROM card WHERE number = ?";

                PreparedStatement pstmt = con.prepareStatement(sql);
                pstmt.setString(1, cardNumber);

                try (ResultSet cardRead = pstmt.executeQuery(sql)) {
                    while (cardRead.next()) {
                        /* --- get balance for this account -- */

                        balance = cardRead.getLong("balance");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                System.out.println("1. Balance\n2. Add income\n3. Do transfer\n4. Close account\n5. Log out\n0. Exit");
                switch (scanner.nextInt()) {

                    case 1:
                        System.out.println("Balance: " + balance);
                        Account(cardNumber);
                        break;
                    case 2:
                        System.out.println("\nEnter income:");
                        String income = scanner.next();

                        sql = "UPDATE card SET number = ?, balance = balance + ?";
                        PreparedStatement pstmt2 = con.prepareStatement(sql);
                        pstmt2.setString(1, cardNumber);
                        pstmt2.setString(2, income);
                        pstmt2.executeUpdate();

                        System.out.println("\nIncome was added!");
                        Account(cardNumber);
                        break;
                    case 3:
                        System.out.println("\nTransfer\nEnter card number:");
                        String cardTransferTo = scanner.next();

                        if (!checkCardLuhn(cardTransferTo)) {
                            System.out.println("Probably you made mistake in the card number. Please try again!");
                            Account(cardNumber);
                        }

                        sql = "SELECT id FROM card WHERE number = '?'";
                        PreparedStatement pstmt3 = con.prepareStatement(sql);
                        pstmt3.setString(1, cardTransferTo);

                        try (ResultSet cardId = pstmt3.executeQuery(sql)) {
                            if (!cardId.next()) {
                                System.out.println("Such a card does not exist.");
                                Account(cardNumber);
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                        if (cardTransferTo.equals(cardNumber)) {
                            System.out.println("You can't transfer money to the same account!");
                            Account(cardNumber);
                        }
                        System.out.println("Enter how much money you want to transfer:");
                        long sumTransfer = scanner.nextLong();

                        if (sumTransfer > balance) {
                            System.out.println("Not enough money!");
                        } else {
                            sql = "UPDATE card SET number = ?, balance = balance + ?";
                            PreparedStatement pstmt4 = con.prepareStatement(sql);
                            pstmt4.setString(1, cardTransferTo);
                            pstmt4.setString(2, String.valueOf(sumTransfer));
                            pstmt4.executeUpdate();
                            sql = "UPDATE card SET number = ?, balance = balance - ?";
                            PreparedStatement pstmt5 = con.prepareStatement(sql);
                            pstmt5.setString(1, cardNumber);
                            pstmt5.setString(2, String.valueOf(sumTransfer));
                            pstmt5.executeUpdate();
                            System.out.println("Success!");
                        }
                        break;
                    case 4:
                        //Connection to SQL
                        PreparedStatement pstmt6 = con.prepareStatement("DELETE FROM card WHERE number = ?");
                        pstmt6.setString(1, cardNumber);
                        pstmt6.executeUpdate();
                        System.out.println("\nThe account has been closed!");
                        break;
                    case 5:
                        System.out.println("\nYou have successfully logged out!\n");
                        mainMenu();
                        break;
                    case 0:
                        exit();
                        break;
                    default:
                        System.out.println("Incorrect choice");
                        break;
                }
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void exit() {
        System.out.println("\nBye!");
    }

    public static void main(String[] args) {
        String url = "jdbc:sqlite:" + args[1];

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl(url);

        try (Connection con = dataSource.getConnection()) {
            try (Statement statement = con.createStatement()) {

                statement.executeUpdate("CREATE TABLE card (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "number TEXT," +
                        "pin TEXT," +
                        "balance INTEGER DEFAULT 0)");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        mainMenu();
    }
}
