package com.amdocs.telecom.dao;

import com.amdocs.telecom.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginHistoryDAO {

    public static void recordLoginAttempt(String identifier, String role, String status, String reason) {
        String query = "INSERT INTO login_history (identifier, role, login_status, failure_reason) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, identifier);
            pstmt.setString(2, role);
            pstmt.setString(3, status);
            pstmt.setString(4, reason);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[!] Warning: Could not log login history: " + e.getMessage());
        }
    }

    public static void displayRecentLogins() {
        String query = "SELECT history_id, identifier, role, login_status, failure_reason, login_time " +
                       "FROM login_history ORDER BY history_id DESC LIMIT 10";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            System.out.println("\n==========================================================================================");
            System.out.println("                                RECENT SYSTEM LOGIN AUDIT TRAIL");
            System.out.println("==========================================================================================");
            System.out.printf("%-6s | %-24s | %-16s | %-10s | %-16s | %-20s\n", 
                              "ID", "User Identifier", "Role", "Status", "Reason", "Timestamp");
            System.out.println("------------------------------------------------------------------------------------------");

            while (rs.next()) {
                System.out.printf("%-6d | %-24s | %-16s | %-10s | %-16s | %-20s\n",
                    rs.getInt("history_id"),
                    rs.getString("identifier"),
                    rs.getString("role"),
                    rs.getString("login_status"),
                    rs.getString("failure_reason") != null ? rs.getString("failure_reason") : "-",
                    rs.getTimestamp("login_time").toString()
                );
            }
            System.out.println("------------------------------------------------------------------------------------------");
        } catch (SQLException e) {
            System.err.println("Error fetching login history: " + e.getMessage());
        }
    }
}