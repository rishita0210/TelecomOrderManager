package com.amdocs.telecom.dao;

import com.amdocs.telecom.util.DBConnection;
import java.sql.*;

public class OrderDAO {

    public boolean placeOrder(int customerId, String productCode, double amount, String paymentMode) {
        String orderQuery = "INSERT INTO telecom_order (order_number, customer_id, order_status, total_amount) VALUES (?, ?, 'CONFIRMED', ?)";
        String itemQuery = "INSERT INTO order_item (order_id, product_id, quantity, unit_price) SELECT ?, product_id, 1, ? FROM telecom_product WHERE product_code = ?";
        String paymentQuery = "INSERT INTO order_payment (order_id, transaction_reference, amount, payment_mode, payment_status) VALUES (?, ?, ?, ?, 'SUCCESS')";

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

            // Commit Transaction
            conn.commit();
            System.out.println("\n[✔] Order Placed Successfully! Order Number: " + orderNumber);
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
