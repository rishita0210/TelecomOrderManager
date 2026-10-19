package com.amdocs.telecom.main;

import com.amdocs.telecom.dao.CustomerDAO;
import com.amdocs.telecom.dao.OrderDAO;
import com.amdocs.telecom.dao.ProductDAO;
import com.amdocs.telecom.dao.ProvisioningDAO;
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
    private static final OrderDAO orderDAO = new OrderDAO();
    private static final ProvisioningDAO provisioningDAO = new ProvisioningDAO();

    public static void main(String[] args) {
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            System.err.println("CRITICAL ERROR: Could not connect to MySQL. Please check your database settings.");
            return;
        }

        boolean running = true;
        while (running) {
            printHeader("TELECOM ORDER MANAGEMENT & PROVISIONING SYSTEM");
            System.out.println("1. Customer Portal");
            System.out.println("2. Administrator Portal");
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
                    System.out.println("\n[!] Invalid choice. Please enter 1, 2, or 3.");
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
            System.out.println("3. Place New Telecom Order");
            System.out.println("4. Track Order & Activate Service");
            System.out.println("5. Back to Main Menu");
            System.out.print("Enter your choice (1-5): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    registerCustomer();
                    break;
                case "2":
                    browseProducts();
                    break;
                case "3":
                    placeOrderWorkflow();
                    break;
                case "4":
                    trackAndActivateOrder();
                    break;
                case "5":
                    back = true;
                    break;
                default:
                    System.out.println("\n[!] Invalid choice. Please enter a number between 1 and 5.");
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
        System.out.print("\nPress Enter to return to menu...");
        scanner.nextLine();
    }

    private static void browseProducts() {
        printHeader("TELECOM PRODUCT CATALOG");
        printProductTable();
        System.out.print("\nPress Enter to return to menu...");
        scanner.nextLine();
    }

    private static void printProductTable() {
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
    }

    private static void placeOrderWorkflow() {
        printHeader("PLACE TELECOM ORDER");
        try {
            System.out.print("Enter your Customer ID (e.g., 1): ");
            int customerId = Integer.parseInt(scanner.nextLine().trim());

            printHeader("TELECOM PRODUCT CATALOG");
            printProductTable();

            System.out.print("\nEnter Product Code to order (e.g., PROD1001): ");
            String productCode = scanner.nextLine().trim();

            TelecomProduct selectedProduct = productDAO.getProductByCode(productCode);
            double price = 0.0;
            if (selectedProduct != null) {
                price = selectedProduct.getMonthlyPrice();
                System.out.printf("Selected: %s | Amount Due: $%.2f\n", selectedProduct.getProductName(), price);
            } else {
                System.out.print("Enter Monthly Price for this product: ");
                price = Double.parseDouble(scanner.nextLine().trim());
            }

            System.out.print("Enter Payment Mode (UPI / CARD / NET_BANKING): ");
            String paymentMode = scanner.nextLine().trim().toUpperCase();

            boolean success = orderDAO.placeOrder(customerId, productCode, price, paymentMode);
            if (!success) {
                System.out.println("\n[X] Order creation failed. Please check customer ID and product code.");
            }
        } catch (NumberFormatException e) {
            System.out.println("\n[X] Invalid input format. Please enter numbers where required.");
        }
        System.out.print("\nPress Enter to return to menu...");
        scanner.nextLine();
    }

    private static void trackAndActivateOrder() {
        printHeader("ORDER TRACKING & NETWORK ACTIVATION");
        try {
            System.out.print("Enter Order ID to activate (e.g., 4): ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                System.out.println("\n[!] Input cannot be empty.");
            } else {
                long orderId = Long.parseLong(input);
                boolean success = provisioningDAO.completeProvisioning(orderId);
                if (success) {
                    System.out.printf("\n[✔] SUCCESS: Network nodes configured! Order #%d is now ACTIVATED.\n", orderId);
                } else {
                    System.out.println("\n[X] Activation failed. Check that the Order ID exists in the database.");
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("\n[X] Invalid Order ID format. Please enter a valid number.");
        }
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