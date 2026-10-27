package com.amdocs.telecom.dao;

import com.amdocs.telecom.exception.InvalidOrderOperationException;
import com.amdocs.telecom.model.OrderItem;
import com.amdocs.telecom.model.TelecomOrder;
import com.amdocs.telecom.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    // 1. Single-Item Checkout Method
    public long placeOrder(int customerId, String productCode, double amount, String paymentMode) {
        String orderQuery = "INSERT INTO telecom_order (order_number, customer_id, order_type, order_status, total_amount) VALUES (?, ?, ?, 'CONFIRMED', ?)";
        String itemQuery = "INSERT INTO order_item (order_id, product_id, quantity, unit_price, discount, tax, total_amount) " +
                           "SELECT ?, product_id, 1, ?, 0.00, ROUND(? * 0.18, 2), ROUND(? * 1.18, 2) " +
                           "FROM telecom_product WHERE product_code = ?";
        String paymentQuery = "INSERT INTO order_payment (order_id, transaction_reference, amount, payment_mode, payment_status) VALUES (?, ?, ?, ?, 'SUCCESS')";
        String provQuery = "INSERT INTO provisioning_request (order_id, service_type, provisioning_status, network_element_id) VALUES (?, ?, 'PENDING', 'NODE-AMDOCS-CORE-01')";
        String reserveInvQuery = "UPDATE inventory_item SET status = 'RESERVED', assigned_order_id = ? WHERE item_type = ? AND status = 'AVAILABLE' LIMIT 1";

        String orderNumber = "ORD-" + System.currentTimeMillis();
        String txRef = "TXN-" + System.currentTimeMillis();

        Connection conn = DBConnection.getConnection();
        if (conn == null) return -1;

        try {
            conn.setAutoCommit(false);

            String orderType = "NEW_CONNECTION";
            String hardwareType = "SIM";
            String provType = "MOBILE_SERVICE";

            if (productCode.startsWith("PROD2")) {
                orderType = "BROADBAND";
                hardwareType = "ROUTER";
                provType = "BROADBAND";
            } else if (productCode.startsWith("PROD3")) {
                orderType = "ENTERPRISE";
                hardwareType = "ONT";
                provType = "VPN";
            } else if (productCode.contains("5G")) {
                provType = "5G_SERVICE";
            }

            long generatedOrderId = -1;
            try (PreparedStatement orderStmt = conn.prepareStatement(orderQuery, Statement.RETURN_GENERATED_KEYS)) {
                orderStmt.setString(1, orderNumber);
                orderStmt.setInt(2, customerId);
                orderStmt.setString(3, orderType);
                orderStmt.setDouble(4, amount * 1.18);
                orderStmt.executeUpdate();

                ResultSet rs = orderStmt.getGeneratedKeys();
                if (rs.next()) {
                    generatedOrderId = rs.getLong(1);
                } else {
                    conn.rollback();
                    return -1;
                }
            }

            try (PreparedStatement itemStmt = conn.prepareStatement(itemQuery)) {
                itemStmt.setLong(1, generatedOrderId);
                itemStmt.setDouble(2, amount);
                itemStmt.setDouble(3, amount);
                itemStmt.setDouble(4, amount);
                itemStmt.setString(5, productCode);
                itemStmt.executeUpdate();
            }

            try (PreparedStatement payStmt = conn.prepareStatement(paymentQuery)) {
                payStmt.setLong(1, generatedOrderId);
                payStmt.setString(2, txRef);
                payStmt.setDouble(3, amount * 1.18);
                payStmt.setString(4, paymentMode);
                payStmt.executeUpdate();
            }

            try (PreparedStatement provStmt = conn.prepareStatement(provQuery)) {
                provStmt.setLong(1, generatedOrderId);
                provStmt.setString(2, provType);
                provStmt.executeUpdate();
            }

            try (PreparedStatement invStmt = conn.prepareStatement(reserveInvQuery)) {
                invStmt.setLong(1, generatedOrderId);
                invStmt.setString(2, hardwareType);
                int rows = invStmt.executeUpdate();
                if (rows > 0) {
                    System.out.printf("[✔] Inventory Allocation: Reserved 1 %s for Order #%d\n", hardwareType, generatedOrderId);
                } else {
                    System.out.printf("[!] Inventory Notice: %s out of stock. Placed on backorder.\n", hardwareType);
                }
            }

            conn.commit();
            return generatedOrderId;

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            System.err.println("[X] Transaction rolled back: " + e.getMessage());
            return -1;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    // 2. Multi-Item Cart Checkout (Section 6)
    public boolean placeMultiItemOrder(int customerId, List<OrderItem> cartItems, List<String> productCodes, double grandTotal, String paymentMode) {
        String orderQuery = "INSERT INTO telecom_order (order_number, customer_id, order_type, order_status, total_amount) VALUES (?, ?, ?, 'CONFIRMED', ?)";
        String itemQuery = "INSERT INTO order_item (order_id, product_id, quantity, unit_price, discount, tax, total_amount) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String paymentQuery = "INSERT INTO order_payment (order_id, transaction_reference, amount, payment_mode, payment_status) VALUES (?, ?, ?, ?, 'SUCCESS')";
        String provQuery = "INSERT INTO provisioning_request (order_id, service_type, provisioning_status, network_element_id) VALUES (?, ?, 'PENDING', 'NODE-AMDOCS-CORE-01')";
        String reserveInvQuery = "UPDATE inventory_item SET status = 'RESERVED', assigned_order_id = ? WHERE item_type = ? AND status = 'AVAILABLE' LIMIT 1";

        String orderNumber = "ORD-" + System.currentTimeMillis();
        String txRef = "TXN-" + System.currentTimeMillis();

        Connection conn = DBConnection.getConnection();
        if (conn == null) return false;

        try {
            conn.setAutoCommit(false);

            String orderType = "NEW_CONNECTION";
            if (productCodes.stream().anyMatch(c -> c.startsWith("PROD2"))) {
                orderType = "BROADBAND";
            } else if (productCodes.stream().anyMatch(c -> c.startsWith("PROD3"))) {
                orderType = "ENTERPRISE";
            } else if (productCodes.stream().anyMatch(c -> c.contains("ESIM"))) {
                orderType = "ESIM_ACTIVATION";
            }

            long generatedOrderId = -1;
            try (PreparedStatement orderStmt = conn.prepareStatement(orderQuery, Statement.RETURN_GENERATED_KEYS)) {
                orderStmt.setString(1, orderNumber);
                orderStmt.setInt(2, customerId);
                orderStmt.setString(3, orderType);
                orderStmt.setDouble(4, grandTotal);
                orderStmt.executeUpdate();

                ResultSet rs = orderStmt.getGeneratedKeys();
                if (rs.next()) {
                    generatedOrderId = rs.getLong(1);
                } else {
                    conn.rollback();
                    return false;
                }
            }

            try (PreparedStatement itemStmt = conn.prepareStatement(itemQuery)) {
                for (OrderItem item : cartItems) {
                    itemStmt.setLong(1, generatedOrderId);
                    itemStmt.setInt(2, item.getProductId());
                    itemStmt.setInt(3, item.getQuantity());
                    itemStmt.setDouble(4, item.getUnitPrice());
                    itemStmt.setDouble(5, item.getDiscount());
                    itemStmt.setDouble(6, item.getTax());
                    itemStmt.setDouble(7, item.getTotalAmount());
                    itemStmt.addBatch();
                }
                itemStmt.executeBatch();
            }

            try (PreparedStatement payStmt = conn.prepareStatement(paymentQuery)) {
                payStmt.setLong(1, generatedOrderId);
                payStmt.setString(2, txRef);
                payStmt.setDouble(3, grandTotal);
                payStmt.setString(4, paymentMode);
                payStmt.executeUpdate();
            }

            try (PreparedStatement provStmt = conn.prepareStatement(provQuery)) {
                provStmt.setLong(1, generatedOrderId);
                provStmt.setString(2, orderType);
                provStmt.executeUpdate();
            }

            for (String code : productCodes) {
                String reqHardware = "SIM";
                if (code.startsWith("PROD2")) reqHardware = "ROUTER";
                else if (code.startsWith("PROD3")) reqHardware = "ONT";
                else if (code.contains("ESIM")) reqHardware = "ESIM";

                try (PreparedStatement invStmt = conn.prepareStatement(reserveInvQuery)) {
                    invStmt.setLong(1, generatedOrderId);
                    invStmt.setString(2, reqHardware);
                    int updated = invStmt.executeUpdate();
                    if (updated > 0) {
                        System.out.printf("[✔] Inventory Allocation: Reserved 1 %s for Order #%d\n", reqHardware, generatedOrderId);
                    }
                }
            }

            conn.commit();
            System.out.println("\n[✔] Multi-Item Order Placed Successfully! Order Number: " + orderNumber);
            return true;

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            System.err.println("[X] Multi-Item Order failed, transaction rolled back: " + e.getMessage());
            return false;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    // 3. Section 13: Order Cancellation with Business State Machine & Custom Exception
    public boolean cancelOrder(long orderId, boolean isAdmin) throws InvalidOrderOperationException {
        String checkStatusQuery = "SELECT order_status FROM telecom_order WHERE order_id = ?";
        String updateOrderQuery = "UPDATE telecom_order SET order_status = 'CANCELLED' WHERE order_id = ?";
        String releaseInventoryQuery = "UPDATE inventory_item SET status = 'AVAILABLE', assigned_order_id = NULL WHERE assigned_order_id = ?";
        String cancelProvQuery = "UPDATE provisioning_request SET provisioning_status = 'CANCELLED' WHERE order_id = ?";

        Connection conn = DBConnection.getConnection();
        if (conn == null) return false;

        try {
            try (PreparedStatement checkStmt = conn.prepareStatement(checkStatusQuery)) {
                checkStmt.setLong(1, orderId);
                ResultSet rs = checkStmt.executeQuery();
                if (!rs.next()) {
                    throw new InvalidOrderOperationException("Order ID #" + orderId + " not found in the system.");
                }
                String currentStatus = rs.getString("order_status");

                if ("ACTIVATED".equalsIgnoreCase(currentStatus) || "COMPLETED".equalsIgnoreCase(currentStatus)) {
                    throw new InvalidOrderOperationException(
                        "Cannot cancel Order #" + orderId + " in " + currentStatus + " status. Active services require service termination / disconnection workflow.");
                }

                if ("PROVISIONING".equalsIgnoreCase(currentStatus) && !isAdmin) {
                    throw new InvalidOrderOperationException(
                        "Order #" + orderId + " is currently in PROVISIONING. Cancellation requires Administrator approval.");
                }

                if ("CANCELLED".equalsIgnoreCase(currentStatus)) {
                    throw new InvalidOrderOperationException("Order #" + orderId + " is already in CANCELLED status.");
                }
            }

            conn.setAutoCommit(false);

            try (PreparedStatement ordStmt = conn.prepareStatement(updateOrderQuery)) {
                ordStmt.setLong(1, orderId);
                ordStmt.executeUpdate();
            }

            int releasedCount = 0;
            try (PreparedStatement invStmt = conn.prepareStatement(releaseInventoryQuery)) {
                invStmt.setLong(1, orderId);
                releasedCount = invStmt.executeUpdate();
            }

            try (PreparedStatement provStmt = conn.prepareStatement(cancelProvQuery)) {
                provStmt.setLong(1, orderId);
                provStmt.executeUpdate();
            }

            conn.commit();
            System.out.printf("\n[✔] SUCCESS: Order #%d has been CANCELLED.\n", orderId);
            if (releasedCount > 0) {
                System.out.printf("[✔] Inventory Released: %d hardware unit(s) returned to AVAILABLE stock.\n", releasedCount);
            }
            return true;

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            System.err.println("[X] Cancellation failed, rolled back: " + e.getMessage());
            return false;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    // 4. View Customer Past Orders
    public void displayCustomerOrders(int customerId) {
        String query = "SELECT o.order_id, o.order_number, o.order_type, o.order_status, o.total_amount, o.order_date " +
                       "FROM telecom_order o WHERE o.customer_id = ? ORDER BY o.order_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, customerId);
            ResultSet rs = pstmt.executeQuery();

            System.out.println("\n-------------------------------------------------------------------------------------------");
            System.out.printf("%-8s | %-20s | %-16s | %-12s | %-10s | %-20s\n", 
                "Order ID", "Order Number", "Order Type", "Status", "Total ($)", "Order Date");
            System.out.println("-------------------------------------------------------------------------------------------");

            boolean hasOrders = false;
            while (rs.next()) {
                hasOrders = true;
                System.out.printf("%-8d | %-20s | %-16s | %-12s | $%-9.2f | %-20s\n",
                    rs.getInt("order_id"),
                    rs.getString("order_number"),
                    rs.getString("order_type"),
                    rs.getString("order_status"),
                    rs.getDouble("total_amount"),
                    rs.getTimestamp("order_date").toString()
                );
            }

            if (!hasOrders) {
                System.out.println("No past orders found for this account.");
            }
            System.out.println("-------------------------------------------------------------------------------------------");

        } catch (SQLException e) {
            System.err.println("Error fetching customer orders: " + e.getMessage());
        }
    }

    // Section 15, Option 5: Step-by-Step Visual Lifecycle Pipeline
    public void trackOrderVisualPipeline(long orderId) {
        String query = "SELECT o.order_id, o.order_number, o.order_status, p.payment_status, pr.provisioning_status " +
                       "FROM telecom_order o " +
                       "LEFT JOIN order_payment p ON o.order_id = p.order_id " +
                       "LEFT JOIN provisioning_request pr ON o.order_id = pr.order_id " +
                       "WHERE o.order_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setLong(1, orderId);
            ResultSet rs = pstmt.executeQuery();
            if (!rs.next()) {
                System.out.println("\n[X] Order ID #" + orderId + " not found.");
                return;
            }

            String ordStatus = rs.getString("order_status");
            String payStatus = rs.getString("payment_status");
            String provStatus = rs.getString("provisioning_status");

            System.out.println("\n==========================================================================");
            System.out.printf(" TRACKING AUDIT FOR ORDER #%d (%s)\n", orderId, rs.getString("order_number"));
            System.out.println("==========================================================================");

            boolean isCancelled = "CANCELLED".equalsIgnoreCase(ordStatus);
            boolean isActivated = "ACTIVATED".equalsIgnoreCase(ordStatus) || "COMPLETED".equalsIgnoreCase(ordStatus);

            System.out.println("[✔] Step 1: Order Created");
            System.out.println("[✔] Step 2: Validated (KYC & Product restrictions passed)");
            System.out.println("PAYMENT_PENDING".equalsIgnoreCase(payStatus) ? "[ ] Step 3: Payment Pending" : "[✔] Step 3: Payment Confirmed");
            System.out.println(isCancelled ? "[X] Step 4: Inventory Released (Order Cancelled)" : "[✔] Step 4: Inventory Reserved");
            
            if (isCancelled) {
                System.out.println("[X] Step 5: Provisioning Aborted");
                System.out.println("[X] Step 6: Order Terminated (CANCELLED)");
            } else if (isActivated) {
                System.out.println("[✔] Step 5: Provisioning Completed (Network handshake OK)");
                System.out.println("[✔] Step 6: Activated (Live on NODE-AMDOCS-CORE-01)");
                System.out.println("[✔] Step 7: Completed (Customer Subscription Active)");
            } else {
                System.out.println("[➔] Step 5: Provisioning In-Progress (Pending network allocation)");
                System.out.println("[ ] Step 6: Activated");
                System.out.println("[ ] Step 7: Completed");
            }
            System.out.println("==========================================================================");

        } catch (SQLException e) {
            System.err.println("Error tracking order: " + e.getMessage());
        }
    }

    // Section 15, Option 7: View Active Subscriptions
    public void displayActiveServices(int customerId) {
        String query = "SELECT s.subscription_id, p.product_name, p.product_type, s.status, s.start_date " +
                       "FROM customer_subscription s " +
                       "JOIN telecom_product p ON s.product_id = p.product_id " +
                       "WHERE s.customer_id = ? AND s.status = 'ACTIVE'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, customerId);
            ResultSet rs = pstmt.executeQuery();

            System.out.println("\n======================= ACTIVE SUBSCRIBER SERVICES =======================");
            System.out.printf("%-16s | %-24s | %-16s | %-12s | %-20s\n", 
                "Subscription ID", "Plan / Service", "Type", "Status", "Start Date");
            System.out.println("--------------------------------------------------------------------------");

            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("SUB-%-12d | %-24s | %-16s | %-12s | %-20s\n",
                    rs.getInt("subscription_id"),
                    rs.getString("product_name"),
                    rs.getString("product_type"),
                    rs.getString("status"),
                    rs.getTimestamp("start_date").toString()
                );
            }
            if (!found) {
                System.out.println("No active live services found. Place an order to subscribe!");
            }
            System.out.println("==========================================================================");
        } catch (SQLException e) {
            System.err.println("Error fetching active services: " + e.getMessage());
        }
    }
}