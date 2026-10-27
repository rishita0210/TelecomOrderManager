package com.amdocs.telecom.main;

import com.amdocs.telecom.dao.*;
import com.amdocs.telecom.exception.CustomerNotEligibleException;
import com.amdocs.telecom.exception.DuplicateOrderException;
import com.amdocs.telecom.exception.InvalidOrderException;
import com.amdocs.telecom.exception.ProductUnavailableException;
import com.amdocs.telecom.model.Customer;
import com.amdocs.telecom.model.FieldEngineer;
import com.amdocs.telecom.model.TelecomProduct;
import com.amdocs.telecom.service.OrderValidationService;
import com.amdocs.telecom.service.ProvisioningService;
import com.amdocs.telecom.util.CaptchaGenerator;
import com.amdocs.telecom.util.DBConnection;
import com.amdocs.telecom.util.OTPService;
import com.amdocs.telecom.util.PaymentFactory;
import com.amdocs.telecom.util.PaymentProcessor;
import com.amdocs.telecom.service.AnalyticsService;
import com.amdocs.telecom.service.AdminService;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    private static final AnalyticsService analyticsService = new AnalyticsService();
    private static final AdminService adminService = new AdminService();
    
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
                    System.out.println("\n[!] Invalid choice. Please enter a number between 1 and 5.");
            }
        }
    }
    // =========================================================================
    // AUTHENTICATION & SECURITY (Page 2: CAPTCHA, OTP, Account Locking)
    // =========================================================================
    
        private static void handleCustomerLogin() {
        printHeader("CUSTOMER LOGIN & AUTHENTICATION");

        if (failedAttempts >= 3) {
            System.out.println("\n[X] ACCOUNT LOCKED: Exceeded maximum 3 failed attempts. Please use Option 6 (Forgot Password) to unlock.");
            LoginHistoryDAO.recordLoginAttempt("UNKNOWN/LOCKED", "CUSTOMER", "LOCKED", "MAX_ATTEMPTS_EXCEEDED");
            return;
        }

        System.out.print("Enter Registered Email: ");
        String email = scanner.nextLine().trim();

        Customer customer = customerDAO.getCustomerByEmail(email);
        if (customer == null) {
            failedAttempts++;
            LoginHistoryDAO.recordLoginAttempt(email, "CUSTOMER", "FAILED", "USER_NOT_FOUND");
            System.out.printf("\n[X] Authentication failed: User not found. (%d/3 attempts used)\n", failedAttempts);
            if (failedAttempts >= 3) {
                LoginHistoryDAO.recordLoginAttempt(email, "CUSTOMER", "LOCKED", "ACCOUNT_LOCKED_3_ATTEMPTS");
            }
            return;
        }

        System.out.print("Enter Password: ");
        String password = scanner.nextLine().trim();

        if (!customer.getPassword().equals(password)) {
            failedAttempts++;
            LoginHistoryDAO.recordLoginAttempt(email, "CUSTOMER", "FAILED", "BAD_PASSWORD");
            System.out.printf("\n[X] Authentication failed: Invalid Password. (%d/3 attempts used)\n", failedAttempts);
            if (failedAttempts >= 3) {
                LoginHistoryDAO.recordLoginAttempt(email, "CUSTOMER", "LOCKED", "ACCOUNT_LOCKED_3_ATTEMPTS");
            }
            return;
        }

        // 1. CAPTCHA Verification
        String generatedCaptcha = CaptchaGenerator.generateCaptcha();
        System.out.println("\n[SECURITY CHECK] CAPTCHA CODE: >>> " + generatedCaptcha + " <<<");
        System.out.print("Enter CAPTCHA exactly as shown: ");
        String enteredCaptcha = scanner.nextLine().trim();

        if (!generatedCaptcha.equals(enteredCaptcha)) {
            failedAttempts++;
            LoginHistoryDAO.recordLoginAttempt(email, "CUSTOMER", "FAILED", "INVALID_CAPTCHA");
            System.out.printf("\n[X] Security verification failed: Invalid CAPTCHA. (%d/3 attempts used)\n", failedAttempts);
            if (failedAttempts >= 3) {
                LoginHistoryDAO.recordLoginAttempt(email, "CUSTOMER", "LOCKED", "ACCOUNT_LOCKED_3_ATTEMPTS");
            }
            return;
        }

        // 2. Simulated 2-Factor OTP Verification
        String generatedOTP = OTPService.generateOTP();
        System.out.println("\n[SMS/EMAIL SIMULATOR] 2FA One-Time Password (OTP): " + generatedOTP);
        System.out.print("Enter 6-digit OTP: ");
        String enteredOTP = scanner.nextLine().trim();

        if (!generatedOTP.equals(enteredOTP)) {
            failedAttempts++;
            LoginHistoryDAO.recordLoginAttempt(email, "CUSTOMER", "FAILED", "INVALID_OTP");
            System.out.printf("\n[X] 2FA verification failed: Incorrect OTP. (%d/3 attempts used)\n", failedAttempts);
            if (failedAttempts >= 3) {
                LoginHistoryDAO.recordLoginAttempt(email, "CUSTOMER", "LOCKED", "ACCOUNT_LOCKED_3_ATTEMPTS");
            }
            return;
        }

        // Authentication Successful: Reset counter and log SUCCESS
        failedAttempts = 0;
        LoginHistoryDAO.recordLoginAttempt(email, "CUSTOMER", "SUCCESS", null);
        
        System.out.printf("\n[✔] Login Successful! Welcome back, %s (Customer ID: %d)\n", 
            customer.getCustomerName(), customer.getCustomerId());
        
        handleCustomerDashboard(customer.getCustomerId());
    }

    // =========================================================================
    // CUSTOMER DASHBOARD
    // =========================================================================
    private static void handleForgotPassword() {
        printHeader("FORGOT PASSWORD / ACCOUNT RECOVERY");
        System.out.print("Enter Registered Email: ");
        String email = scanner.nextLine().trim();

        Customer customer = customerDAO.getCustomerByEmail(email);
        if (customer == null) {
            System.out.println("\n[X] No customer account found matching that email.");
            System.out.print("\nPress Enter to return...");
            scanner.nextLine();
            return;
        }

        String resetOTP = OTPService.generateOTP();
        System.out.println("\n[DISPATCH] Recovery OTP dispatched to " + customer.getMobileNumber() + ": >>> " + resetOTP + " <<<");
        System.out.print("Enter 6-digit OTP to verify identity: ");
        String enteredOTP = scanner.nextLine().trim();

        if (!resetOTP.equals(enteredOTP)) {
            System.out.println("\n[X] OTP verification failed. Password reset aborted.");
            System.out.print("\nPress Enter to return...");
            scanner.nextLine();
            return;
        }

        System.out.print("Enter New Password: ");
        String newPassword = scanner.nextLine().trim();

        if (newPassword.isEmpty()) {
            System.out.println("\n[X] Password cannot be blank.");
        } else {
            boolean updated = customerDAO.updatePassword(email, newPassword);
            if (updated) {
                failedAttempts = 0; // Reset any previous account locks
                System.out.println("\n[✔] SUCCESS: Password reset successfully! Your account is unlocked.");
            } else {
                System.out.println("\n[X] Database error updating password.");
            }
        }

        System.out.print("\nPress Enter to return to main menu...");
        scanner.nextLine();
    }

    private static void registerCustomer() {
        printHeader("CUSTOMER REGISTRATION");
        try {
            // Automatically generate a unique customer business account code:
            String custNo = "CUST-" + (System.currentTimeMillis() % 1000000);
            System.out.println("[*] Generated Account ID: " + custNo);

            System.out.print("Enter Full Name: ");
            String name = scanner.nextLine().trim();

            System.out.print("Enter Email Address: ");
            String email = scanner.nextLine().trim();

            System.out.print("Enter Password: ");
            String password = scanner.nextLine().trim();

            System.out.print("Enter Mobile Number: ");
            String mobile = scanner.nextLine().trim();

            System.out.print("Enter Customer Type (INDIVIDUAL / SME / ENTERPRISE): ");
            String type = scanner.nextLine().trim().toUpperCase();

            System.out.print("Enter Address: ");
            String address = scanner.nextLine().trim();

            System.out.print("Enter City: ");
            String city = scanner.nextLine().trim();

            Customer customer = new Customer(custNo, name, email, password, mobile, type, address, city);
            boolean success = customerDAO.addCustomer(customer);

            if (success) {
                System.out.println("\n[✔] SUCCESS: Registration complete! Account Number: " + custNo);
                System.out.println("You can now log in using Option 1 with email: " + email);
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

            // 1. Fetch Product
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

            // 2. Section 7 Validations (Customer, Product, Documents, Eligibility)
            Customer customer = customerDAO.getCustomerById(customerId);
            OrderValidationService validator = new OrderValidationService();
            validator.validateOrder(customer, selectedProduct, paymentMode);

            // 3. Payment Processing via Factory Pattern
            PaymentProcessor processor = PaymentFactory.getProcessor(paymentMode);
            if (processor != null) {
                processor.process(price);
            }

            // 4. Place Order Transaction
            long newOrderId = orderDAO.placeOrder(customerId, productCode, price, paymentMode);

            if (newOrderId > 0) {
                // Auto-dispatch field engineer if on-site installation is required
                if (productCode.startsWith("PROD2") || productCode.startsWith("PROD3")) {
                    System.out.println("\n[!] Service requires on-site installation. Dispatching field engineer...");
                    Optional<FieldEngineer> assigned = engineerDAO.assignLeastBusyEngineer("Bangalore");
                    assigned.ifPresent(eng -> {
                        System.out.printf("[✔] Technician Assigned: %s (Current active load: %d tasks)\n", 
                            eng.getEngineerName(), eng.getActiveTasks() + 1);
                    });
                }

                // Dispatched to background thread pool for simulated activation
                System.out.println("[*] Provisioning request dispatched to background worker pool...");
                provisioningService.triggerAsyncProvisioning(newOrderId);
            } else {
                System.out.println("\n[X] Order creation failed.");
            }

        } catch (CustomerNotEligibleException | ProductUnavailableException | 
                 DuplicateOrderException | InvalidOrderException e) {
            System.out.println("\n[VALIDATION REJECTED] " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("\n[X] Invalid input format. Please enter numbers where required.");
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
    // EXPORT ORDERS WORKFLOW
    // =========================================================================
    // private static void exportOrdersWorkflow() {
    //     printHeader("EXPORT REPORT TO CSV");
    //     String fileName = "orders_report.csv";
    //     boolean success = reportDAO.exportOrdersToCSV(fileName);
    //     if (success) {
    //         System.out.println("\n[✔] SUCCESS: Report exported successfully as " + fileName);
    //     } else {
    //         System.out.println("\n[X] Export failed. Check database connection or file permissions.");
    //     }
    //     System.out.print("\nPress Enter to return to menu...");
    //     scanner.nextLine();
    // }

    // =========================================================================
    // ADMINISTRATOR & ROLE PORTALS (Page 2: Role-Based Authorization)
    // =========================================================================
     
    // =========================================================================
    // SECTION 16: ADMINISTRATOR DASHBOARD (10 OPTIONS)
    // =========================================================================
    // =========================================================================
    // ADMINISTRATOR PORTAL & DASHBOARD (Section 16 Implementation)
    // =========================================================================
    private static void handleAdminPortal() {
        // Log access by the Administrator
        LoginHistoryDAO.recordLoginAttempt("admin@telecom.local", "ORDER_ADMIN", "SUCCESS", null);

        boolean back = false;
        while (!back) {
            printHeader("ORDER ADMINISTRATOR DASHBOARD");
            System.out.println("1. Customer Management (Active Accounts & Profiles)");
            System.out.println("2. Product Management (Catalogue & Pricing)");
            System.out.println("3. Order Management (Live Orders & Statuses)");
            System.out.println("4. Inventory Management (Stock Levels & Low-Stock Alerts)");
            System.out.println("5. Provisioning Management (Active Network Tickets)");
            System.out.println("6. Payment Management (Audited Transactions)");
            System.out.println("7. Failed / Cancelled Orders");
            System.out.println("8. Security Audit Logs (Login History)");
            System.out.println("9. Reports & CSV Export");
            System.out.println("10. Logout to Main Menu");
            System.out.print("Enter your choice (1-10): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    printHeader("CUSTOMER MANAGEMENT");
                    displayAllCustomers();
                    pauseForUser();
                    break;

                case "2":
                    browseProducts();
                    break;

                case "3":
                    printHeader("ORDER MANAGEMENT");
                    reportDAO.displaySystemAnalytics();
                    pauseForUser();
                    break;

                case "4":
                    printHeader("INVENTORY MANAGEMENT & AUDIT");
                    reportDAO.displaySystemAnalytics();
                    pauseForUser();
                    break;

                case "5":
                    printHeader("PROVISIONING MANAGEMENT");
                    trackAndActivateOrder();
                    break;

                case "6":
                    printHeader("PAYMENT AUDIT TRAIL");
                    displayPaymentAuditTrail();
                    pauseForUser();
                    break;

                case "7":
                    printHeader("FAILED / CANCELLED ORDERS");
                    displayCancelledOrders();
                    pauseForUser();
                    break;

                case "8":
                    // Section 2: Persistent Login History Audit Trail
                    LoginHistoryDAO.displayRecentLogins();
                    pauseForUser();
                    break;

                case "9":
                    printHeader("REPORTS & EXPORT");
                    exportOrdersWorkflow();
                    pauseForUser();
                    break;

                case "10":
                    back = true;
                    System.out.println("\n[✔] Administrator logged out successfully.");
                    break;

                default:
                    System.out.println("\n[!] Invalid choice. Please enter a number between 1 and 10.");
            }
        }
    }

    // --- Helper Methods Supporting Admin Dashboard ---

    private static void displayAllCustomers() {
        String query = "SELECT customer_id, customer_number, customer_name, email, mobile_number, customer_type, account_status FROM customer";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
            System.out.printf("%-6s | %-12s | %-18s | %-22s | %-12s | %-12s | %-10s\n", 
                "ID", "Cust No", "Name", "Email", "Mobile", "Type", "Status");
            System.out.println("---------------------------------------------------------------------------------------------------------");
            while (rs.next()) {
                System.out.printf("%-6d | %-12s | %-18s | %-22s | %-12s | %-12s | %-10s\n",
                    rs.getInt("customer_id"),
                    rs.getString("customer_number"),
                    rs.getString("customer_name"),
                    rs.getString("email"),
                    rs.getString("mobile_number"),
                    rs.getString("customer_type"),
                    rs.getString("account_status")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching customer data: " + e.getMessage());
        }
    }

    private static void displayPaymentAuditTrail() {
        String query = "SELECT payment_id, order_id, transaction_reference, amount, payment_mode, payment_status, payment_date FROM order_payment ORDER BY payment_id DESC LIMIT 10";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
            System.out.printf("%-6s | %-8s | %-22s | %-10s | %-12s | %-10s | %-20s\n", 
                "ID", "Order ID", "Txn Ref", "Amount ($)", "Mode", "Status", "Date");
            System.out.println("---------------------------------------------------------------------------------------------------");
            while (rs.next()) {
                System.out.printf("%-6d | %-8d | %-22s | $%-9.2f | %-12s | %-10s | %-20s\n",
                    rs.getInt("payment_id"),
                    rs.getInt("order_id"),
                    rs.getString("transaction_reference"),
                    rs.getDouble("amount"),
                    rs.getString("payment_mode"),
                    rs.getString("payment_status"),
                    rs.getTimestamp("payment_date").toString()
                );
            }
        } catch (SQLException e) {
            System.err.println("Error querying payments: " + e.getMessage());
        }
    }

    private static void displayCancelledOrders() {
        String query = "SELECT order_id, order_number, customer_id, total_amount, order_date FROM telecom_order WHERE order_status = 'CANCELLED'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            
            System.out.printf("%-8s | %-20s | %-12s | %-12s | %-20s\n", 
                "Order ID", "Order Number", "Customer ID", "Amount ($)", "Order Date");
            System.out.println("--------------------------------------------------------------------------------");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%-8d | %-20s | %-12d | $%-11.2f | %-20s\n",
                    rs.getInt("order_id"),
                    rs.getString("order_number"),
                    rs.getInt("customer_id"),
                    rs.getDouble("total_amount"),
                    rs.getTimestamp("order_date").toString()
                );
            }
            if (!found) {
                System.out.println("No cancelled or failed orders found in database.");
            }
        } catch (SQLException e) {
            System.err.println("Error querying cancelled orders: " + e.getMessage());
        }
    }

    private static void exportOrdersWorkflow() {
        String fileName = "orders_report.csv";
        boolean success = reportDAO.exportOrdersToCSV(fileName);
        if (success) {
            System.out.println("\n[✔] SUCCESS: Report exported successfully as " + fileName);
        } else {
            System.out.println("\n[X] Export failed.");
        }
    }

    private static void pauseForUser() {
        System.out.print("\nPress Enter to return to menu...");
        scanner.nextLine();
    }

    private static void handleProductManagementSubMenu() {
        printHeader("PRODUCT MANAGEMENT");
        System.out.println("1. View All Products");
        System.out.println("2. Add New Telecom Product");
        System.out.println("3. Back");
        System.out.print("Enter choice (1-3): ");
        String sub = scanner.nextLine().trim();

        if ("1".equals(sub)) {
            browseProducts();
        } else if ("2".equals(sub)) {
            try {
                System.out.print("Enter Product Code (e.g., PROD4001): ");
                String code = scanner.nextLine().trim();
                System.out.print("Enter Product Name: ");
                String name = scanner.nextLine().trim();
                System.out.print("Enter Product Type (BROADBAND / MOBILE_PLAN / ENTERPRISE): ");
                String type = scanner.nextLine().trim().toUpperCase();
                System.out.print("Enter Monthly Price: ");
                double price = Double.parseDouble(scanner.nextLine().trim());

                boolean ok = adminService.addProduct(code, name, type, price);
                if (ok) {
                    System.out.println("\n[✔] Product successfully added to catalog!");
                } else {
                    System.out.println("\n[X] Failed to add product (duplicate product code).");
                }
            } catch (Exception e) {
                System.out.println("\n[X] Invalid input: " + e.getMessage());
            }
            pauseForAdmin();
        }
    }

    private static void handleReportsSubMenu() {
        printHeader("REPORTS & ANALYTICS");
        System.out.println("1. View System Analytics (SQL Aggregations)");
        System.out.println("2. View Advanced Stream Analytics (Java 8 Streams)");
        System.out.println("3. Export Orders Report to CSV");
        System.out.println("4. Back");
        System.out.print("Enter choice (1-4): ");
        String rChoice = scanner.nextLine().trim();

        switch (rChoice) {
            case "1":
                reportDAO.displaySystemAnalytics();
                pauseForAdmin();
                break;
            case "2":
                analyticsService.runFullStreamAnalytics();
                pauseForAdmin();
                break;
            case "3":
                exportOrdersWorkflow();
                break;
            default:
                break;
        }
    }

    private static void pauseForAdmin() {
        System.out.print("\nPress Enter to return to Administrator Dashboard...");
        scanner.nextLine();
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