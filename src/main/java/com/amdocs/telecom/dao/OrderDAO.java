package com.amdocs.telecom.dao;

import com.amdocs.telecom.util.DBConnection;
import java.sql.*;

public class OrderDAO {

    
    public long placeOrder(int customerId, String productCode, double amount, String paymentMode) {
        String orderQuery = "INSERT INTO telecom_order (order_number, customer_id, order_status, total_amount) VALUES (?, ?, 'CONFIRMED', ?)";
        String itemQuery = "INSERT INTO order_item (order_id, product_id, quantity, unit_price) SELECT ?, product_id, 1, ? FROM telecom_product WHERE product_code = ?";
        String paymentQuery = "INSERT INTO order_payment (order_id, transaction_reference, amount, payment_mode, payment_status) VALUES (?, ?, ?, ?, 'SUCCESS')";
        String provisioningQuery = "INSERT INTO provisioning_request (order_id, service_type, provisioning_status, network_element_id) VALUES (?, ?, 'PENDING', 'NODE-AMDOCS-CORE-01')";
        String productTypeQuery = "SELECT product_type FROM telecom_product WHERE product_code = ?";
        String reserveInventoryQuery = "UPDATE inventory_item SET status = 'RESERVED', assigned_order_id = ? " +
                                       "WHERE item_type = ? AND status = 'AVAILABLE' LIMIT 1";

        String orderNumber = "ORD-" + System.currentTimeMillis();
        String txRef = "TXN-" + System.currentTimeMillis();

        Connection conn = DBConnection.getConnection();
        if (conn == null) return -1;

        try {
            conn.setAutoCommit(false);

            // 1. Insert Order & retrieve generated order_id
            long generatedOrderId = -1;
            try (PreparedStatement orderStmt = conn.prepareStatement(orderQuery, Statement.RETURN_GENERATED_KEYS)) {
                orderStmt.setString(1, orderNumber);
                orderStmt.setInt(2, customerId);
                orderStmt.setDouble(3, amount);
                orderStmt.executeUpdate();

                ResultSet rs = orderStmt.getGeneratedKeys();
                if (rs.next()) {
                    generatedOrderId = rs.getLong(1);
                } else {
                    conn.rollback();
                    return -1;
                }
            }

            // 2. Insert Order Item
            try (PreparedStatement itemStmt = conn.prepareStatement(itemQuery)) {
                itemStmt.setLong(1, generatedOrderId);
                itemStmt.setDouble(2, amount);
                itemStmt.setString(3, productCode);
                itemStmt.executeUpdate();
            }

            // 3. Insert Payment Record
            try (PreparedStatement payStmt = conn.prepareStatement(paymentQuery)) {
                payStmt.setLong(1, generatedOrderId);
                payStmt.setString(2, txRef);
                payStmt.setDouble(3, amount);
                payStmt.setString(4, paymentMode);
                payStmt.executeUpdate();
            }

            // 4. Determine Hardware Requirement
            String requiredItemType = "SIM";
            try (PreparedStatement pTypeStmt = conn.prepareStatement(productTypeQuery)) {
                pTypeStmt.setString(1, productCode);
                ResultSet rs = pTypeStmt.executeQuery();
                if (rs.next()) {
                    String pType = rs.getString("product_type");
                    if ("BROADBAND".equalsIgnoreCase(pType)) {
                        requiredItemType = "ROUTER";
                    } else if ("ENTERPRISE".equalsIgnoreCase(pType)) {
                        requiredItemType = "ONT";
                    }
                }
            }

            // 5. Create Network Provisioning Ticket
            try (PreparedStatement provStmt = conn.prepareStatement(provisioningQuery)) {
                provStmt.setLong(1, generatedOrderId);
                provStmt.setString(2, requiredItemType);
                provStmt.executeUpdate();
                System.out.printf("[✔] Network Engine: Provisioning ticket generated (PENDING) for Order #%d\n", generatedOrderId);
            }

            // 6. Reserve Hardware
            try (PreparedStatement invStmt = conn.prepareStatement(reserveInventoryQuery)) {
                invStmt.setLong(1, generatedOrderId);
                invStmt.setString(2, requiredItemType);
                int rowsUpdated = invStmt.executeUpdate();
                if (rowsUpdated > 0) {
                    System.out.printf("[✔] Inventory Allocation: Reserved 1 %s for Order #%d\n", requiredItemType, generatedOrderId);
                } else {
                    System.out.printf("[!] Inventory Notice: No available %s in stock. Order placed on backorder.\n", requiredItemType);
                }
            }

            conn.commit();
            System.out.println("[✔] Order Placed Successfully! Order Number: " + orderNumber);
            return generatedOrderId; // Returns the valid Order ID (e.g., 7 or 8)

        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            System.err.println("[X] Order placement failed, transaction rolled back: " + e.getMessage());
            return -1;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException e) { e.printStackTrace(); }
        }
    }
    // Case Study Section 13: Order Cancellation with Hardware Release
    public boolean cancelOrder(long orderId) {
        String checkStatusQuery = "SELECT order_status FROM telecom_order WHERE order_id = ?";
        String updateOrderQuery = "UPDATE telecom_order SET order_status = 'CANCELLED' WHERE order_id = ?";
        String releaseInventoryQuery = "UPDATE inventory_item SET status = 'AVAILABLE', assigned_order_id = NULL WHERE assigned_order_id = ?";
        String cancelProvQuery = "UPDATE provisioning_request SET provisioning_status = 'CANCELLED' WHERE order_id = ?";

        Connection conn = DBConnection.getConnection();
        if (conn == null) return false;

        try {
            // 1. Check current order status
            try (PreparedStatement checkStmt = conn.prepareStatement(checkStatusQuery)) {
                checkStmt.setLong(1, orderId);
                ResultSet rs = checkStmt.executeQuery();
                if (!rs.next()) {
                    System.out.println("[X] Cancellation Error: Order ID not found.");
                    return false;
                }
                String currentStatus = rs.getString("order_status");
                if ("ACTIVATED".equalsIgnoreCase(currentStatus) || "COMPLETED".equalsIgnoreCase(currentStatus)) {
                    System.out.println("[X] Cancellation Rejected: Service is already ACTIVATED. Disconnection request required.");
                    return false;
                }
                if ("CANCELLED".equalsIgnoreCase(currentStatus)) {
                    System.out.println("[!] Order is already CANCELLED.");
                    return false;
                }
            }

            // Start atomic transaction
            conn.setAutoCommit(false);

            // 2. Mark order as CANCELLED
            try (PreparedStatement ordStmt = conn.prepareStatement(updateOrderQuery)) {
                ordStmt.setLong(1, orderId);
                ordStmt.executeUpdate();
            }

            // 3. Release reserved hardware back to AVAILABLE in warehouse
            int releasedCount = 0;
            try (PreparedStatement invStmt = conn.prepareStatement(releaseInventoryQuery)) {
                invStmt.setLong(1, orderId);
                releasedCount = invStmt.executeUpdate();
            }

            // 4. Cancel any open provisioning ticket
            try (PreparedStatement provStmt = conn.prepareStatement(cancelProvQuery)) {
                provStmt.setLong(1, orderId);
                provStmt.executeUpdate();
            }

            conn.commit();
            System.out.printf("[✔] SUCCESS: Order #%d has been CANCELLED.\n", orderId);
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
    // Fetch and display all past orders for a customer using SQL JOINs
    public void displayCustomerOrders(int customerId) {
        String query = "SELECT o.order_id, o.order_number, p.product_name, o.total_amount, " +
                       "o.order_status, o.order_date, pay.payment_mode " +
                       "FROM telecom_order o " +
                       "JOIN order_item oi ON o.order_id = oi.order_id " +
                       "JOIN telecom_product p ON oi.product_id = p.product_id " +
                       "LEFT JOIN order_payment pay ON o.order_id = pay.order_id " +
                       "WHERE o.customer_id = ? " +
                       "ORDER BY o.order_date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, customerId);
            ResultSet rs = pstmt.executeQuery();

            boolean found = false;
            System.out.printf("%-8s %-22s %-20s %-10s %-12s %-12s\n", 
                "Order ID", "Order Number", "Product", "Amount", "Status", "Payment");
            System.out.println("-----------------------------------------------------------------------------------------");

            while (rs.next()) {
                found = true;
                System.out.printf("%-8d %-22s %-20s $%-9.2f %-12s %-12s\n",
                    rs.getInt("order_id"),
                    rs.getString("order_number"),
                    rs.getString("product_name"),
                    rs.getDouble("total_amount"),
                    rs.getString("order_status"),
                    rs.getString("payment_mode") != null ? rs.getString("payment_mode") : "N/A");
            }

            if (!found) {
                System.out.println("No past orders found for Customer ID: " + customerId);
            }
            System.out.println("-----------------------------------------------------------------------------------------");

        } catch (SQLException e) {
            System.err.println("Error fetching customer orders: " + e.getMessage());
        }
    }
}