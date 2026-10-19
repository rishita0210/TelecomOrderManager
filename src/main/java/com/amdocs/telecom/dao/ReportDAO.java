package com.amdocs.telecom.dao;

import com.amdocs.telecom.util.DBConnection;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReportDAO {

    public void displaySystemAnalytics() {
        String revenueQuery = "SELECT SUM(total_amount) AS total_revenue, COUNT(*) AS total_orders FROM telecom_order";
        String statusBreakdownQuery = "SELECT order_status, COUNT(*) AS count FROM telecom_order GROUP BY order_status";
        String inventoryQuery = "SELECT item_type, status, COUNT(*) AS count FROM inventory_item GROUP BY item_type, status";

        try (Connection conn = DBConnection.getConnection()) {
            // 1. Total Sales & Orders
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(revenueQuery)) {
                if (rs.next()) {
                    System.out.printf("Total Orders Placed: %d\n", rs.getInt("total_orders"));
                    System.out.printf("Total Gross Revenue: $%.2f\n", rs.getDouble("total_revenue"));
                }
            }

            System.out.println("--------------------------------------------------");
            System.out.println("Orders by Status:");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(statusBreakdownQuery)) {
                while (rs.next()) {
                    System.out.printf(" - %-15s : %d\n", rs.getString("order_status"), rs.getInt("count"));
                }
            }

            System.out.println("--------------------------------------------------");
            System.out.println("Inventory Stock Audit:");
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(inventoryQuery)) {
                while (rs.next()) {
                    System.out.printf(" - %-10s [%-10s] : %d\n", 
                        rs.getString("item_type"), rs.getString("status"), rs.getInt("count"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching analytics: " + e.getMessage());
        }
    }

    public boolean exportOrdersToCSV(String fileName) {
        String query = "SELECT order_id, order_number, customer_id, order_status, total_amount, order_date FROM telecom_order";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query);
             PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {

            // CSV Header
            writer.println("OrderID,OrderNumber,CustomerID,Status,TotalAmount,OrderDate");

            while (rs.next()) {
                writer.printf("%d,%s,%d,%s,%.2f,%s\n",
                    rs.getInt("order_id"),
                    rs.getString("order_number"),
                    rs.getInt("customer_id"),
                    rs.getString("order_status"),
                    rs.getDouble("total_amount"),
                    rs.getTimestamp("order_date"));
            }
            return true;
        } catch (SQLException | IOException e) {
            System.err.println("Error exporting CSV: " + e.getMessage());
            return false;
        }
    }
}