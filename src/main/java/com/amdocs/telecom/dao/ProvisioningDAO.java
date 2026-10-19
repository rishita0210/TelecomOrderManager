package com.amdocs.telecom.dao;

import com.amdocs.telecom.util.DBConnection;
import java.sql.*;

public class ProvisioningDAO {

    // 1. Create a provisioning request when an order is confirmed
    public boolean createProvisioningRequest(long orderId, String serviceType) {
        String query = "INSERT INTO provisioning_request (order_id, service_type, provisioning_status, network_element_id) " +
                       "VALUES (?, ?, 'PENDING', 'NODE-AMDOCS-CORE-01')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setLong(1, orderId);
            pstmt.setString(2, serviceType);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error creating provisioning request: " + e.getMessage());
            return false;
        }
    }

    // 2. Activate / Complete the provisioning
    public boolean completeProvisioning(long orderId) {
        String updateProvisioning = "UPDATE provisioning_request SET provisioning_status = 'COMPLETED', completion_time = CURRENT_TIMESTAMP WHERE order_id = ?";
        String updateOrder = "UPDATE telecom_order SET order_status = 'ACTIVATED' WHERE order_id = ?";

        Connection conn = DBConnection.getConnection();
        if (conn == null) return false;

        try {
            conn.setAutoCommit(false);

            try (PreparedStatement pStmt = conn.prepareStatement(updateProvisioning)) {
                pStmt.setLong(1, orderId);
                pStmt.executeUpdate();
            }

            try (PreparedStatement oStmt = conn.prepareStatement(updateOrder)) {
                oStmt.setLong(1, orderId);
                oStmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            System.err.println("Error activating provisioning: " + e.getMessage());
            return false;
        } finally {
            try { conn.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }
}
