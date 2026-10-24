package com.amdocs.telecom.main;

import com.amdocs.telecom.dao.*;
import com.amdocs.telecom.model.Customer;
import com.amdocs.telecom.model.FieldEngineer;
import com.amdocs.telecom.model.TelecomProduct;
import com.amdocs.telecom.service.ProvisioningService;
import com.amdocs.telecom.util.CaptchaGenerator;
import com.amdocs.telecom.util.DBConnection;
import com.amdocs.telecom.util.OTPService;
import com.amdocs.telecom.util.PaymentFactory;
import com.amdocs.telecom.util.PaymentProcessor;


import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class MainApp {
    private static final Scanner scanner = new Scanner(System.in);
    private static final CustomerDAO customerDAO = new CustomerDAO();
    private static final ProductDAO productDAO = new ProductDAO();
    private static final OrderDAO orderDAO = new OrderDAO();
    private static final ProvisioningDAO provisioningDAO = new ProvisioningDAO();
    private static final ReportDAO reportDAO = new ReportDAO();
    private static final EngineerDAO engineerDAO = new EngineerDAO();
    private static final ProvisioningService provisioningService = new ProvisioningService();
    
    // Track consecutive failed login attempts for account locking
    private static int failedAttempts = 0;

    public static void main(String[] args) {
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            System.err.println("CRITICAL ERROR: Could not connect to MySQL. Please check your database settings.");
            return;
        }

        boolean running = true;
        while (running) {
            printHeader("TELECOM ORDER & PROVISIONING SYSTEM");
            System.out.println("1. Customer Login");
            System.out.println("2. Customer Registration");
            System.out.println("3. Order Administrator");
            System.out.println("4. Provisioning Engineer");
            System.out.println("5. Inventory Administrator");
            System.out.println("6. Forgot Password");
            System.out.println("7. Exit");
            System.out.print("Enter your choice (1-7): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    handleCustomerLogin();
                    break;
                case "2":
                    registerCustomer();
                    break;
                case "3":
                    handleAdminPortal();
                    break;
                case "4":
                    handleProvisioningEngineerPortal();
                    break;
                case "5":
                    handleInventoryAdminPortal();
                    break;
                case "6":
                    handleForgotPassword();
                    break;
                case "7":
                    running = false;
                    provisioningService.shutdown();
                    System.out.println("\nThank you for using the Telecom System. Goodbye!");
                    break;
                default:
                    System.out.println("\n[!] Invalid choice. Please enter a number between 1 and 7.");
            }
        }
        scanner.close();
    }

    // =========================================================================
    // AUTHENTICATION & SECURITY (Page 2: CAPTCHA, OTP, Account Locking)
    // =========================================================================
    private static void handleCustomerLogin() {
        printHeader("CUSTOMER LOGIN & AUTHENTICATION");

        if (failedAttempts >= 3) {
            System.out.println("\n[X] ACCOUNT LOCKED: Exceeded maximum 3 failed attempts. Contact administrator.");
            return;
        }

        System.out.print("Enter Registered Email: ");
        String email = scanner.nextLine().trim();

        Customer customer = customerDAO.getCustomerByEmail(email);
        if (customer == null) {
            failedAttempts++;
            System.out.printf("\n[X] Authentication failed: User not found. (%d/3 attempts used)\n", failedAttempts);
            return;
        }

        // 1. CAPTCHA Verification
        String generatedCaptcha = CaptchaGenerator.generateCaptcha();
        System.out.println("\n[SECURITY CHECK] CAPTCHA CODE: >>> " + generatedCaptcha + " <<<");
        System.out.print("Enter CAPTCHA exactly as shown: ");
        String enteredCaptcha = scanner.nextLine().trim();

        if (!generatedCaptcha.equals(enteredCaptcha)) {
            failedAttempts++;
            System.out.printf("\n[X] Security verification failed: Invalid CAPTCHA. (%d/3 attempts used)\n", failedAttempts);
            return;
        }

        // 2. Simulated 2-Factor OTP Verification
        String generatedOTP = OTPService.generateOTP();
        System.out.println("\n[SMS/EMAIL SIMULATOR] 2FA One-Time Password (OTP): " + generatedOTP);
        System.out.print("Enter 6-digit OTP: ");
        String enteredOTP = scanner.nextLine().trim();

        if (!generatedOTP.equals(enteredOTP)) {
            failedAttempts++;
            System.out.printf("\n[X] 2FA verification failed: Incorrect OTP. (%d/3 attempts used)\n", failedAttempts);
            return;
        }

        // Successful authentication
        failedAttempts = 0;
        System.out.printf("\n[✔] Login Successful! Welcome back, %s (Customer ID: %d)\n", 
            customer.getCustomerName(), customer.getCustomerId());
        
        handleCustomerDashboard(customer.getCustomerId());
    }

    private static void handleForgotPassword() {
        printHeader("PASSWORD RECOVERY / OTP RESET");
        System.out.print("Enter Registered Mobile or Email: ");
        String contact = scanner.nextLine().trim();

        String resetOTP = OTPService.generateOTP();
        System.out.println("\n[DISPATCH] Recovery OTP dispatched to " + contact + ": >>> " + resetOTP + " <<<");
        System.out.print("Enter the OTP to verify identity: ");
        String entered = scanner.nextLine().trim();

        if (resetOTP.equals(entered)) {
            failedAttempts = 0; // Unlock account
            System.out.println("\n[✔] Identity verified! Account has been unlocked. Please proceed to login.");
        } else {
            System.out.println("\n[X] OTP verification failed.");
        }
        System.out.print("\nPress Enter to return to main menu...");
        scanner.nextLine();
    }

    // =========================================================================
    // CUSTOMER DASHBOARD
    // =========================================================================
    private static void handleCustomerDashboard(int customerId) {
        boolean logout = false;
        while (!logout) {
            printHeader("CUSTOMER DASHBOARD");
            System.out.println("1. Browse Telecom Product Catalog");
            System.out.println("2. Place New Telecom Order");
            System.out.println("3. Track Order & Activate Service");
            System.out.println("4. View My Past Orders");
            System.out.println("5. Logout to Main Menu");
            System.out.print("Enter your choice (1-5): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    browseProducts();
                    break;
                case "2":
                    placeOrderWorkflow(customerId);
                    break;
                case "3":
                    trackAndActivateOrder();
                    break;
                case "4":
                    orderDAO.displayCustomerOrders(customerId);
                    System.out.print("\nPress Enter to return...");
                    scanner.nextLine();
                    break;
                case "5":
                    logout = true;
                    System.out.println("\nLogged out successfully.");
                    break;
                default:
                    System.out.println("\n[!] Invalid choice. Try again.");
            }
        }
    }

    private static void registerCustomer() {
        printHeader("CUSTOMER REGISTRATION");
        try {
            // Auto-generate customer number using current system timestamp
        String custNo = "CUST-" + (System.currentTimeMillis() % 100000);
        System.out.println("Assigned Customer Number: " + custNo);

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
                System.out.println("\n[✔] SUCCESS: Registration complete! You can now log in via Option 1.");
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

   
        private static void placeOrderWorkflow(int customerId) {
        printHeader("PLACE TELECOM ORDER");
        try {
            printProductTable();

            System.out.print("\nEnter Product Code to order (e.g., PROD1001): ");
            String productCode = scanner.nextLine().trim();

            TelecomProduct selectedProduct = productDAO.getProductByCode(productCode);
            if (selectedProduct == null) {
                System.out.println("\n[X] Invalid Product Code. Order canceled.");
                System.out.print("\nPress Enter to return to menu...");
                scanner.nextLine();
                return;
            }

            double price = selectedProduct.getMonthlyPrice();
            System.out.printf("Selected: %s | Amount Due: $%.2f\n", selectedProduct.getProductName(), price);

            System.out.print("Enter Payment Mode (UPI / CARD / NET_BANKING): ");
            String paymentMode = scanner.nextLine().trim().toUpperCase();

            PaymentProcessor processor = PaymentFactory.getProcessor(paymentMode);
            if (processor == null) {
                System.out.println("\n[X] Invalid Payment Mode. Must be UPI, CARD, or NET_BANKING.");
                System.out.print("\nPress Enter to return to menu...");
                scanner.nextLine();
                return;
            }
            processor.process(price);

            // Returns the database-generated Order ID (e.g., 8)
            long newOrderId = orderDAO.placeOrder(customerId, productCode, price, paymentMode);

            if (newOrderId > 0) {
                // 1. Dispatch technician for on-site services
                if (productCode.startsWith("PROD2") || productCode.startsWith("PROD3")) {
                    System.out.println("\n[!] Service requires on-site installation. Dispatching field engineer...");
                    Optional<FieldEngineer> assigned = engineerDAO.assignLeastBusyEngineer("Bangalore");
                    assigned.ifPresent(eng -> {
                        System.out.printf("[✔] Technician Assigned: %s (Current active load: %d tasks)\n", 
                            eng.getEngineerName(), eng.getActiveTasks() + 1);
                    });
                }

                // 2. Submit task to the background ExecutorService thread pool
                System.out.println("[*] Provisioning request dispatched to background worker pool...");
                provisioningService.triggerAsyncProvisioning(newOrderId);

            } else {
                System.out.println("\n[X] Order creation failed.");
            }
        } catch (NumberFormatException e) {
            System.out.println("\n[X] Invalid input format. Please enter valid numbers.");
        }
        System.out.print("\nPress Enter to return to menu...");
        scanner.nextLine();
    }

    private static void trackAndActivateOrder() {
        printHeader("ORDER TRACKING & NETWORK ACTIVATION");
        try {
            System.out.print("Enter Order ID to activate: ");
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
            System.out.println("\n[X] Invalid Order ID format.");
        }
        System.out.print("\nPress Enter to return to menu...");
        scanner.nextLine();
    }

    // =========================================================================
    // ADMINISTRATOR & ROLE PORTALS (Page 2: Role-Based Authorization)
    // =========================================================================
    private static void handleAdminPortal() {
        boolean back = false;
        while (!back) {
            printHeader("ORDER ADMINISTRATOR PORTAL");
            System.out.println("1. View System Analytics & Inventory Audit");
            System.out.println("2. Export Orders Report to CSV");
            System.out.println("3. Back to Main Menu");
            System.out.print("Enter your choice (1-3): ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    printHeader("SYSTEM ANALYTICS & AUDIT");
                    reportDAO.displaySystemAnalytics();
                    System.out.print("\nPress Enter to return...");
                    scanner.nextLine();
                    break;
                case "2":
                    printHeader("EXPORT REPORT TO CSV");
                    String fileName = "orders_report.csv";
                    boolean success = reportDAO.exportOrdersToCSV(fileName);
                    if (success) {
                        System.out.println("[✔] SUCCESS: Report exported successfully as " + fileName);
                    } else {
                        System.out.println("[X] Export failed.");
                    }
                    System.out.print("\nPress Enter to return...");
                    scanner.nextLine();
                    break;
                case "3":
                    back = true;
                    break;
                default:
                    System.out.println("\n[!] Invalid choice.");
            }
        }
    }

    private static void handleProvisioningEngineerPortal() {
        printHeader("PROVISIONING ENGINEER PORTAL");
        System.out.println("Role: Field Technician / Network Engineer");
        System.out.println("Status: Active connections monitored via OLT-NODE-01");
        trackAndActivateOrder();
    }

    private static void handleInventoryAdminPortal() {
        printHeader("INVENTORY ADMINISTRATOR PORTAL");
        System.out.println("Warehouse Stock Status:");
        reportDAO.displaySystemAnalytics();
        System.out.print("\nPress Enter to return to main menu...");
        scanner.nextLine();
    }

    private static void printHeader(String title) {
        System.out.println("\n==================================================");
        System.out.println("     " + title);
        System.out.println("==================================================");
    }
}