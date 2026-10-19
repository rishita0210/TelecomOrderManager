package com.amdocs.telecom.dao;

import com.amdocs.telecom.util.DBConnection;
import java.sql.*;

public class OrderDAO {

    public boolean placeOrder(int customerId, String productCode, double amount, String paymentMode) {
        String orderQuery = "INSERT INTO telecom_order (order_number, customer_id, order_status, total_amount) VALUES (?, ?, 'CONFIRMED', ?)";
        String itemQuery = "INSERT INTO order_item (order_id, product_id, quantity, unit_price) SELECT ?, product_id, 1, ? FROM telecom_product WHERE product_code = ?";
        String paymentQuery = "INSERT INTO order_payment (order_id, transaction_reference, amount, payment_mode, payment_status) VALUES (?, ?, ?, ?, 'SUCCESS')";
        String provisioningQuery = "INSERT INTO provisioning_request (order_id, service_type, provisioning_status, network_element_id) VALUES (?, ?, 'PENDING', 'NODE-AMDOCS-CORE-01')";
        
        // Find product type to determine required hardware
        String productTypeQuery = "SELECT product_type FROM telecom_product WHERE product_code = ?";
        
        // Reserve an available inventory unit for this order
        String reserveInventoryQuery = "UPDATE inventory_item SET status = 'RESERVED', assigned_order_id = ? " +
                                       "WHERE item_type = ? AND status = 'AVAILABLE' LIMIT 1";

        String orderNumber = "ORD-" + System.currentTimeMillis();
        String txRef = "TXN-" + System.currentTimeMillis();

        Connection conn = DBConnection.getConnection();
        if (conn == null) return false;

        try {
            // Start Transaction
            conn.setAutoCommit(false);

            // 1. Insert Order
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
                    return false;
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

            // 4. Reserve Matching Inventory
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

            // 5. Create Network Provisioning Request
            try (PreparedStatement provStmt = conn.prepareStatement(provisioningQuery)) {
                provStmt.setLong(1, generatedOrderId);
                provStmt.setString(2, requiredItemType);
                provStmt.executeUpdate();
                System.out.printf("[✔] Network Engine: Provisioning ticket generated (PENDING) for Order #%d\n", generatedOrderId);
            }

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

            // Commit all changes together
            conn.commit();
            System.out.println("[✔] Order Placed Successfully! Order Number: " + orderNumber);
            return true;

        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            System.err.println("[X] Order placement failed, transaction rolled back: " + e.getMessage());
            return false;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}