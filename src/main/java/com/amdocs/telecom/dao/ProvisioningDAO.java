package com.amdocs.telecom.dao;

import com.amdocs.telecom.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class ProvisioningDAO {

    public boolean completeProvisioning(long orderId) {
        String updateProvisioning = "UPDATE provisioning_request SET provisioning_status = 'COMPLETED', " +
                                    "completion_time = CURRENT_TIMESTAMP WHERE order_id = ?";
        String updateOrder = "UPDATE telecom_order SET order_status = 'ACTIVATED' WHERE order_id = ?";
        String updateInventory = "UPDATE inventory_item SET status = 'INSTALLED' WHERE assigned_order_id = ?";
        String findOrderDetails = "SELECT customer_id FROM telecom_order WHERE order_id = ?";
        String findOrderItems = "SELECT product_id FROM order_item WHERE order_id = ?";
        String createSubscription = "INSERT INTO customer_subscription (customer_id, order_id, product_id, status, start_date) " +
                                    "VALUES (?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP)";

        Connection conn = DBConnection.getConnection();
        if (conn == null) return false;

        try {
            // Start single atomic JDBC transaction for all Section 12 steps
            conn.setAutoCommit(false);

            // 1. Provisioning Completed
            try (PreparedStatement pStmt = conn.prepareStatement(updateProvisioning)) {
                pStmt.setLong(1, orderId);
                pStmt.executeUpdate();
            }

            // 2. Activate Service & Update Order
            try (PreparedStatement oStmt = conn.prepareStatement(updateOrder)) {
                oStmt.setLong(1, orderId);
                int rows = oStmt.executeUpdate();
                if (rows == 0) {
                    conn.rollback();
                    return false;
                }
            }

            // 3. Update Inventory (RESERVED -> INSTALLED)
            try (PreparedStatement invStmt = conn.prepareStatement(updateInventory)) {
                invStmt.setLong(1, orderId);
                invStmt.executeUpdate();
            }

            // 4. Update Customer Subscription
            int customerId = -1;
            try (PreparedStatement findCustStmt = conn.prepareStatement(findOrderDetails)) {
                findCustStmt.setLong(1, orderId);
                ResultSet rs = findCustStmt.executeQuery();
                if (rs.next()) {
                    customerId = rs.getInt("customer_id");
                }
            }

            if (customerId != -1) {
                try (PreparedStatement findItemsStmt = conn.prepareStatement(findOrderItems);
                     PreparedStatement subStmt = conn.prepareStatement(createSubscription)) {
                    findItemsStmt.setLong(1, orderId);
                    ResultSet rsItems = findItemsStmt.executeQuery();
                    while (rsItems.next()) {
                        int prodId = rsItems.getInt("product_id");
                        subStmt.setInt(1, customerId);
                        subStmt.setLong(2, orderId);
                        subStmt.setInt(3, prodId);
                        subStmt.addBatch();
                    }
                    subStmt.executeBatch();
                }
            }

            // Commit all changes atomically
            conn.commit();

            // 5. Generate Notification
            System.out.println("\n========================================================");
            System.out.printf("[NOTIFICATION] SMS & EMAIL SENT: Your service for Order #%d is now ACTIVATED!\n", orderId);
            System.out.printf("[NOTIFICATION] Customer Subscription created and hardware marked as INSTALLED.\n");
            System.out.println("========================================================");

            return true;

        } catch (SQLException e) {
            try { 
                conn.rollback(); 
            } catch (SQLException ex) { 
                ex.printStackTrace(); 
            }
            System.err.println("Error completing service activation transaction: " + e.getMessage());
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