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
            System.out.println("1. View Profile");
            System.out.println("2. Browse Products");
            System.out.println("3. Create Order");
            System.out.println("4. View My Orders");
            System.out.println("5. Track Order");
            System.out.println("6. Make Payment");
            System.out.println("7. View Active Services");
            System.out.println("8. Cancel Order");
            System.out.println("9. Notifications");
            System.out.println("10. Logout");
            System.out.print("Enter your choice (1-10): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    customerDAO.displayCustomerProfile(customerId);
                    pause();
                    break;
                case "2":
                    browseProducts();
                    break;
                case "3":
                    placeOrderWorkflow(customerId);
                    break;
                case "4":
                    orderDAO.displayCustomerOrders(customerId);
                    pause();
                    break;
                case "5":
                    System.out.print("Enter Order ID to track: ");
                    try {
                        long oId = Long.parseLong(scanner.nextLine().trim());
                        orderDAO.trackOrderVisualPipeline(oId);
                    } catch (NumberFormatException e) {
                        System.out.println("[X] Invalid Order ID.");
                    }
                    pause();
                    break;
                case "6":
                    System.out.println("\n[INFO] Payment is integrated at checkout (Option 3) via Payment Gateway.");
                    System.out.println("[✔] All confirmed orders are automatically settled via UPI/Card/NetBanking.");
                    pause();
                    break;
                case "7":
                    orderDAO.displayActiveServices(customerId);
                    pause();
                    break;
                case "8":
                    cancelOrderWorkflow();
                    break;
                case "9":
                    System.out.println("\n====================== NOTIFICATIONS INBOX ======================");
                    System.out.println("[ALERT] Welcome to Telecom Order Management & Network Provisioning System!");
                    System.out.println("[SMS] KYC documents: VERIFIED. You are eligible for 5G & Fiber lines.");
                    System.out.println("[INFO] Network Core NODE-AMDOCS-CORE-01 is fully operational.");
                    System.out.println("=================================================================");
                    pause();
                    break;
                case "10":
                    logout = true;
                    System.out.println("\n[✔] Logged out successfully. Returning to main menu.");
                    break;
                default:
                    System.out.println("\n[!] Invalid choice. Please select 1-10.");
            }
        }
    }

    private static void pause() {
        System.out.print("\nPress Enter to return to Customer Dashboard...");
        scanner.nextLine();
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
        printHeader("PLACE TELECOM ORDER (MULTI-ITEM CART)");
        browseProducts();

        java.util.List<com.amdocs.telecom.model.OrderItem> cartItems = new java.util.ArrayList<>();
        java.util.List<String> productCodes = new java.util.ArrayList<>();
        double grandTotal = 0.0;

        boolean addingMore = true;
        while (addingMore) {
            System.out.print("\nEnter Product Code to add to cart (e.g., PROD1001): ");
            String productCode = scanner.nextLine().trim();

            com.amdocs.telecom.model.TelecomProduct product = productDAO.getProductByCode(productCode);
            if (product == null) {
                System.out.println("[X] Product not found! Please check the code.");
            } else {
                System.out.print("Enter Quantity (e.g., 1): ");
                int qty = 1;
                try {
                    qty = Integer.parseInt(scanner.nextLine().trim());
                    if (qty <= 0) qty = 1;
                } catch (NumberFormatException e) {
                    qty = 1;
                }

                double unitPrice = product.getMonthlyPrice();
                double discount = 0.00;
                double tax = Math.round((unitPrice * qty * 0.18) * 100.0) / 100.0; // 18% Tax
                double lineTotal = (unitPrice * qty) - discount + tax;

                com.amdocs.telecom.model.OrderItem item = new com.amdocs.telecom.model.OrderItem(
                    product.getProductId(), qty, unitPrice, discount, tax
                );
                cartItems.add(item);
                productCodes.add(productCode);
                grandTotal += lineTotal;

                System.out.printf("[+] Added %d × %s (Line Total with 18%% tax: $%.2f)\n", qty, product.getProductName(), lineTotal);
            }

            System.out.print("Would you like to add another product to this order? (Y/N): ");
            String ans = scanner.nextLine().trim();
            if (!"Y".equalsIgnoreCase(ans)) {
                addingMore = false;
            }
        }

        

        if (cartItems.isEmpty()) {
            System.out.println("\n[!] Cart is empty. Order aborted.");
            return;
        }

        System.out.printf("\n================ CART SUMMARY ================\n");
        System.out.printf("Total Line Items: %d\n", cartItems.size());
        System.out.printf("Grand Total (inc. Tax): $%.2f\n", grandTotal);
        System.out.println("==============================================");

        System.out.print("Enter Payment Mode (UPI / CARD / NET_BANKING): ");
        String paymentMode = scanner.nextLine().trim().toUpperCase();

        boolean success = orderDAO.placeMultiItemOrder(customerId, cartItems, productCodes, grandTotal, paymentMode);
        if (success) {
            System.out.println("[*] Provisioning request dispatched to background worker pool...");
        } else {
            System.out.println("\n[X] Order placement failed.");
        }
    }

    private static void cancelOrderWorkflow() {
        printHeader("CANCEL TELECOM ORDER");
        try {
            System.out.print("Enter Order ID to cancel: ");
            long orderId = Long.parseLong(scanner.nextLine().trim());
            
            // Customer initiates cancellation (isAdmin = false)
            orderDAO.cancelOrder(orderId, false);

        } catch (com.amdocs.telecom.exception.InvalidOrderOperationException e) {
            System.out.println("\n[BUSINESS RULE VIOLATION EXCEPTION]");
            System.out.println("[X] " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("\n[X] Invalid Order ID format.");
        }
        System.out.print("\nPress Enter to return to Customer Dashboard...");
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
    // =========================================================================
    // SECTION 16: ADMINISTRATOR DASHBOARD WITH RBAC
    // =========================================================================
    // =========================================================================
    // SECTION 16: ADMINISTRATOR DASHBOARD WITH RBAC
    // =========================================================================
    
    // =========================================================================
    // SECTION 16: ADMINISTRATOR DASHBOARD (10 OPTIONS)
    // =========================================================================
    private static void handleAdminPortal() {
        if (!authenticateStaffRole("ORDER_ADMIN", "Admin@123")) {
            return;
        }

        boolean logout = false;
        while (!logout) {
            printHeader("ADMINISTRATOR DASHBOARD");
            System.out.println("1. Customer Management");
            System.out.println("2. Product Management");
            System.out.println("3. Order Management");
            System.out.println("4. Inventory Management");
            System.out.println("5. Provisioning Management");
            System.out.println("6. Payment Management");
            System.out.println("7. Failed Orders");
            System.out.println("8. Audit Logs");
            System.out.println("9. Reports (SQL Aggregates, Java 8 Streams & CSV Export)");
            System.out.println("10. Logout");
            System.out.print("Enter your choice (1-10): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    adminService.displayAllCustomers();
                    pauseForAdmin();
                    break;
                case "2":
                    handleProductManagementSubMenu();
                    break;
                case "3":
                    adminService.displayAllOrders();
                    pauseForAdmin();
                    break;
                case "4":
                    adminService.displayFullInventory();
                    pauseForAdmin();
                    break;
                case "5":
                    adminService.displayProvisioningRequests();
                    pauseForAdmin();
                    break;
                case "6":
                    adminService.displayPaymentTransactions();
                    pauseForAdmin();
                    break;
                case "7":
                    adminService.displayFailedOrders();
                    pauseForAdmin();
                    break;
                case "8":
                    adminService.displayAuditLogs();
                    pauseForAdmin();
                    break;
                case "9":
                    handleReportsSubMenu();
                    break;
                case "10":
                    logout = true;
                    System.out.println("\n[✔] Admin session logged out successfully.");
                    break;
                default:
                    System.out.println("\n[!] Invalid choice. Please enter a number between 1 and 10.");
            }
        }
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

    

    private static void exportOrdersWorkflow() {
        printHeader("EXPORT REPORT TO CSV");
        String fileName = "orders_report.csv";
        boolean success = reportDAO.exportOrdersToCSV(fileName);
        if (success) {
            System.out.println("\n[✔] SUCCESS: Report exported successfully as " + fileName);
        } else {
            System.out.println("\n[X] Export failed.");
        }
        pauseForAdmin();
    }

    // =========================================================================
    // ROLE 2: PROVISIONING ENGINEER PORTAL
    // =========================================================================
    private static void handleProvisioningEngineerPortal() {
        if (!authenticateStaffRole("PROVISIONING_ENGINEER", "Eng@123")) {
            return;
        }

        boolean logout = false;
        while (!logout) {
            printHeader("PROVISIONING ENGINEER PORTAL");
            System.out.println("Role: Field Operations / Network Element Engineer");
            System.out.println("Active Node Gateway: NODE-AMDOCS-CORE-01");
            System.out.println("--------------------------------------------------");
            System.out.println("1. View All Provisioning Tickets (Active & Pending)");
            System.out.println("2. Activate / Complete Provisioning for Order");
            System.out.println("3. View Field Engineer Workload & Assignments");
            System.out.println("4. Logout to Main Menu");
            System.out.print("Enter your choice (1-4): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    adminService.displayProvisioningRequests();
                    pauseForEngineer();
                    break;

                case "2":
                    trackAndActivateOrder();
                    break;

                case "3":
                    analyticsService.printEngineerWorkload();
                    pauseForEngineer();
                    break;

                case "4":
                    logout = true;
                    System.out.println("\n[✔] Provisioning Engineer logged out successfully.");
                    break;

                default:
                    System.out.println("\n[!] Invalid choice. Please enter 1, 2, 3, or 4.");
            }
        }
    }

    private static void pauseForEngineer() {
        System.out.print("\nPress Enter to return to Provisioning Portal...");
        scanner.nextLine();
    }

    // =========================================================================
    // ROLE 3: INVENTORY ADMINISTRATOR PORTAL
    // =========================================================================
    private static void handleInventoryAdminPortal() {
        if (!authenticateStaffRole("INVENTORY_ADMIN", "Inv@123")) {
            return;
        }

        boolean logout = false;
        while (!logout) {
            printHeader("INVENTORY ADMINISTRATOR PORTAL");
            System.out.println("Role: Warehouse & Hardware Inventory Manager");
            System.out.println("Managed Hubs: Bangalore Central, Mumbai Depot");
            System.out.println("--------------------------------------------------");
            System.out.println("1. Stock Audit Summary (Aggregate Available vs Reserved)");
            System.out.println("2. Check Low-Stock Threshold Alerts");
            System.out.println("3. View All Physical Serialized Units");
            System.out.println("4. Restock / Add New Hardware to Inventory");
            System.out.println("5. Logout to Main Menu");
            System.out.print("Enter your choice (1-5): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    printHeader("WAREHOUSE AGGREGATE AUDIT");
                    reportDAO.displaySystemAnalytics();
                    pauseForInventoryAdmin();
                    break;

                case "2":
                    checkLowStockAlerts();
                    pauseForInventoryAdmin();
                    break;

                case "3":
                    adminService.displayFullInventory();
                    pauseForInventoryAdmin();
                    break;

                case "4":
                    addNewInventoryItemWorkflow();
                    pauseForInventoryAdmin();
                    break;

                case "5":
                    logout = true;
                    System.out.println("\n[✔] Inventory Administrator logged out successfully.");
                    break;

                default:
                    System.out.println("\n[!] Invalid choice. Please enter a number between 1 and 5.");
            }
        }
    }

    // Helper: Check for any hardware types running critically low (<= 1 available)
    private static void checkLowStockAlerts() {
        printHeader("LOW-STOCK THRESHOLD ALERTS");
        String query = "SELECT item_type, COUNT(*) AS available_count " +
                       "FROM inventory_item " +
                       "WHERE status = 'AVAILABLE' " +
                       "GROUP BY item_type";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            boolean alertFound = false;
            while (rs.next()) {
                String type = rs.getString("item_type");
                int count = rs.getInt("available_count");
                if (count <= 1) {
                    alertFound = true;
                    System.out.printf("[CRITICAL ALERT] %-10s stock is dangerously low! Only %d unit(s) available.\n", type, count);
                } else {
                    System.out.printf("[HEALTHY]        %-10s stock level is sufficient (%d units available).\n", type, count);
                }
            }

            if (!alertFound) {
                System.out.println("\n[✔] All inventory categories meet safety stock thresholds.");
            }
        } catch (SQLException e) {
            System.err.println("Error running stock threshold check: " + e.getMessage());
        }
    }

    // Helper: Add new hardware directly from the console
    private static void addNewInventoryItemWorkflow() {
        printHeader("RESTOCK WAREHOUSE HARDWARE");
        try {
            System.out.print("Enter Item Code (e.g., RTR-998811): ");
            String itemCode = scanner.nextLine().trim();

            System.out.print("Enter Item Type (SIM / ESIM / ROUTER / ONT): ");
            String itemType = scanner.nextLine().trim().toUpperCase();

            System.out.print("Enter Hardware Serial Number: ");
            String serialNo = scanner.nextLine().trim();

            System.out.print("Enter Warehouse Location (e.g., Bangalore Central): ");
            String warehouse = scanner.nextLine().trim();

            String query = "INSERT INTO inventory_item (item_code, item_type, serial_number, warehouse, status) VALUES (?, ?, ?, ?, 'AVAILABLE')";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, itemCode);
                pstmt.setString(2, itemType);
                pstmt.setString(3, serialNo);
                pstmt.setString(4, warehouse.isEmpty() ? "Bangalore Central" : warehouse);
                
                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    System.out.printf("\n[✔] SUCCESS: Hardware [%s - %s] added to %s as AVAILABLE.\n", itemType, itemCode, warehouse);
                }
            }
        } catch (Exception e) {
            System.out.println("\n[X] Restocking failed: " + e.getMessage());
        }
    }

    private static void pauseForInventoryAdmin() {
        System.out.print("\nPress Enter to return to Inventory Portal...");
        scanner.nextLine();
    }

    // Role-based credential check (Declared ONCE)
    private static boolean authenticateStaffRole(String requiredRole, String expectedSecret) {
        printHeader("STAFF AUTHENTICATION: " + requiredRole);
        System.out.printf("Access to this portal requires authorized %s credentials.\n", requiredRole);
        System.out.print("Enter Staff Passcode: ");
        String entered = scanner.nextLine().trim();

        if (expectedSecret.equals(entered)) {
            System.out.println("[✔] Authorization Granted. Accessing restricted portal...");
            LoginHistoryDAO.recordLoginAttempt(requiredRole.toLowerCase() + "@telecom.local", requiredRole, "SUCCESS", null);
            return true;
        } else {
            System.out.println("\n[X] ACCESS DENIED: Unauthorized credentials. Incident logged.");
            LoginHistoryDAO.recordLoginAttempt("UNAUTHORIZED_ATTEMPT", requiredRole, "FAILED", "INVALID_STAFF_SECRET");
            return false;
        }
    }

   

    private static void printHeader(String title) {
        System.out.println("\n==================================================");
        System.out.println("     " + title);
        System.out.println("==================================================");
    }
}