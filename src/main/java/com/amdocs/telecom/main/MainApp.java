package com.amdocs.telecom.main;

import com.amdocs.telecom.dao.CustomerDAO;
import com.amdocs.telecom.dao.ProductDAO;
import com.amdocs.telecom.model.Customer;
import com.amdocs.telecom.model.TelecomProduct;
import com.amdocs.telecom.util.DBConnection;

import java.sql.Connection;
import java.util.List;
import java.util.Scanner;

public class MainApp {
    private static final Scanner scanner = new Scanner(System.in);
    private static final CustomerDAO customerDAO = new CustomerDAO();
    private static final ProductDAO productDAO = new ProductDAO();

    public static void main(String[] args) {
        // Test database connection on startup
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            System.err.println("CRITICAL ERROR: Could not connect to MySQL. Please check your database settings.");
            return;
        }

        boolean running = true;
        while (running) {
            printHeader("TELECOM ORDER MANAGEMENT & PROVISIONING SYSTEM");
            System.out.println("1. Customer Portal (Register & Browse Catalog)");
            System.out.println("2. Administrator Portal (View System Reports)");
            System.out.println("3. Exit Application");
            System.out.print("Enter your choice (1-3): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    handleCustomerPortal();
                    break;
                case "2":
                    handleAdminPortal();
                    break;
                case "3":
                    running = false;
                    System.out.println("\nThank you for using the Telecom System. Goodbye!");
                    break;
                default:
                    System.out.println("\n[!] Invalid choice. Please enter a number between 1 and 3.");
            }
        }
        scanner.close();
    }

    private static void handleCustomerPortal() {
        boolean back = false;
        while (!back) {
            printHeader("CUSTOMER PORTAL");
            System.out.println("1. Register New Customer");
            System.out.println("2. Browse Telecom Product Catalog");
            System.out.println("3. Back to Main Menu");
            System.out.print("Enter your choice (1-3): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    registerCustomer();
                    break;
                case "2":
                    browseProducts();
                    break;
                case "3":
                    back = true;
                    break;
                default:
                    System.out.println("\n[!] Invalid choice. Try again.");
            }
        }
    }

    private static void registerCustomer() {
        printHeader("CUSTOMER REGISTRATION");
        try {
            System.out.print("Enter Customer Number (e.g., CUST-102): ");
            String custNo = scanner.nextLine().trim();

            System.out.print("Enter Full Name: ");
            String name = scanner.nextLine().trim();

            System.out.print("Enter Email Address: ");
            String email = scanner.nextLine().trim();

            System.out.print("Enter Mobile Number: ");
            String mobile = scanner.nextLine().trim();

            System.out.print("Enter Customer Type (INDIVIDUAL / SME / ENTERPRISE): ");
            String type = scanner.nextLine().trim().toUpperCase();

            System.out.print("Enter Address: ");
            String address = scanner.nextLine().trim();

            System.out.print("Enter City: ");
            String city = scanner.nextLine().trim();

            Customer customer = new Customer(custNo, name, email, mobile, type, address, city);
            boolean success = customerDAO.addCustomer(customer);

            if (success) {
                System.out.println("\n[✔] SUCCESS: Customer registered successfully in the database!");
            } else {
                System.out.println("\n[X] FAILURE: Registration failed. Email or Mobile might already exist.");
            }
        } catch (Exception e) {
            System.out.println("\n[X] Error during registration: " + e.getMessage());
        }
    }

    private static void browseProducts() {
        printHeader("TELECOM PRODUCT CATALOG");
        List<TelecomProduct> products = productDAO.getAllProducts();

        if (products.isEmpty()) {
            System.out.println("No products found in the catalog.");
            return;
        }

        System.out.printf("%-10s %-25s %-15s %-10s\n", "Code", "Product Name", "Type", "Price ($)");
        System.out.println("------------------------------------------------------------------");
        for (TelecomProduct p : products) {
            System.out.printf("%-10s %-25s %-15s $%-10.2f\n", 
                p.getProductCode(), p.getProductName(), p.getProductType(), p.getMonthlyPrice());
        }
        System.out.println("------------------------------------------------------------------");
        System.out.print("\nPress Enter to return to menu...");
        scanner.nextLine();
    }

    private static void handleAdminPortal() {
        printHeader("ADMINISTRATOR PORTAL");
        System.out.println("Admin analytics and report features go here.");
        System.out.print("Press Enter to return to main menu...");
        scanner.nextLine();
    }

    private static void printHeader(String title) {
        System.out.println("\n==================================================");
        System.out.println("     " + title);
        System.out.println("==================================================");
    }
}