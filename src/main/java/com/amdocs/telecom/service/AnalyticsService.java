package com.amdocs.telecom.service;

import com.amdocs.telecom.util.DBConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

public class AnalyticsService {

    // ==========================================
    // Internal Helper DTOs to hold raw query data
    // ==========================================
    private static class RawOrderItem {
        String productCode;
        String productName;
        double amount;
        String status;
        String customerType;
        String customerName;
        long processingTimeSeconds;

        RawOrderItem(String productCode, String productName, double amount, 
                     String status, String customerType, String customerName, long processingTimeSeconds) {
            this.productCode = productCode;
            this.productName = productName;
            this.amount = amount;
            this.status = status;
            this.customerType = customerType;
            this.customerName = customerName;
            this.processingTimeSeconds = processingTimeSeconds;
        }
    }

    private static class RawInventoryItem {
        String itemType;
        String status;
        String warehouse;

        RawInventoryItem(String itemType, String status, String warehouse) {
            this.itemType = itemType;
            this.status = status;
            this.warehouse = warehouse;
        }
    }

    // ==========================================
    // Data Fetchers
    // ==========================================
    private List<RawOrderItem> fetchAllOrderData() {
        List<RawOrderItem> list = new ArrayList<>();
        String query = "SELECT p.product_code, p.product_name, o.total_amount, o.order_status, " +
                       "c.customer_type, c.customer_name, " +
                       "TIMESTAMPDIFF(SECOND, o.order_date, IFNULL(pr.completion_time, NOW())) AS processing_time " +
                       "FROM telecom_order o " +
                       "JOIN order_item oi ON o.order_id = oi.order_id " +
                       "JOIN telecom_product p ON oi.product_id = p.product_id " +
                       "JOIN customer c ON o.customer_id = c.customer_id " +
                       "LEFT JOIN provisioning_request pr ON o.order_id = pr.order_id";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                list.add(new RawOrderItem(
                    rs.getString("product_code"),
                    rs.getString("product_name"),
                    rs.getDouble("total_amount"),
                    rs.getString("order_status"),
                    rs.getString("customer_type"),
                    rs.getString("customer_name"),
                    rs.getLong("processing_time")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error loading orders for analytics: " + e.getMessage());
        }
        return list;
    }

    private List<RawInventoryItem> fetchAllInventory() {
        List<RawInventoryItem> list = new ArrayList<>();
        String query = "SELECT item_type, status, IFNULL(warehouse_location, 'Bangalore Central') AS warehouse " +
                       "FROM inventory_item";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                list.add(new RawInventoryItem(
                    rs.getString("item_type"),
                    rs.getString("status"),
                    rs.getString("warehouse")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error loading inventory for analytics: " + e.getMessage());
        }
        return list;
    }

    // =========================================================================
    // SECTION 17: ALL 10 JAVA 8 STREAM API IMPLEMENTATIONS
    // =========================================================================

    /**
     * 1. Most ordered products (e.g., Top 3 most ordered)
     */
    public void printMostOrderedProducts(int topN) {
        System.out.println("\n--- [STREAM] Most Ordered Products (Top " + topN + ") ---");
        List<RawOrderItem> orders = fetchAllOrderData();

        orders.stream()
            .collect(Collectors.groupingBy(item -> item.productName, Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(topN)
            .forEach(entry -> System.out.printf(" - %-25s : %d orders\n", entry.getKey(), entry.getValue()));
    }

    /**
     * 2. Revenue by product
     */
    public void printRevenueByProduct() {
        System.out.println("\n--- [STREAM] Revenue by Product ---");
        List<RawOrderItem> orders = fetchAllOrderData();

        orders.stream()
            .collect(Collectors.groupingBy(
                item -> item.productName,
                Collectors.summingDouble(item -> item.amount)
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .forEach(entry -> System.out.printf(" - %-25s : $%.2f\n", entry.getKey(), entry.getValue()));
    }

    /**
     * 3. Orders by status
     */
    public void printOrdersByStatus() {
        System.out.println("\n--- [STREAM] Orders by Status ---");
        List<RawOrderItem> orders = fetchAllOrderData();

        orders.stream()
            .collect(Collectors.groupingBy(item -> item.status, Collectors.counting()))
            .forEach((status, count) -> System.out.printf(" - %-15s : %d orders\n", status, count));
    }

    /**
     * 4. Orders by customer type (INDIVIDUAL / SME / ENTERPRISE)
     */
    public void printOrdersByCustomerType() {
        System.out.println("\n--- [STREAM] Orders by Customer Type ---");
        List<RawOrderItem> orders = fetchAllOrderData();

        orders.stream()
            .collect(Collectors.groupingBy(item -> item.customerType, Collectors.counting()))
            .forEach((type, count) -> System.out.printf(" - %-15s : %d orders\n", type, count));
    }

    /**
     * 5. Inventory utilization (Percentage of reserved vs total)
     */
    public void printInventoryUtilization() {
        System.out.println("\n--- [STREAM] Inventory Utilization ---");
        List<RawInventoryItem> items = fetchAllInventory();

        long total = items.size();
        if (total == 0) {
            System.out.println("No inventory items found.");
            return;
        }

        Map<String, Long> statusCounts = items.stream()
            .collect(Collectors.groupingBy(item -> item.status, Collectors.counting()));

        statusCounts.forEach((status, count) -> {
            double percentage = (count * 100.0) / total;
            System.out.printf(" - %-12s : %d items (%.1f%%)\n", status, count, percentage);
        });
    }

    /**
     * 6. Available inventory by warehouse
     */
    public void printAvailableInventoryByWarehouse() {
        System.out.println("\n--- [STREAM] Available Inventory by Warehouse ---");
        List<RawInventoryItem> items = fetchAllInventory();

        items.stream()
            .filter(item -> "AVAILABLE".equalsIgnoreCase(item.status))
            .collect(Collectors.groupingBy(item -> item.warehouse, Collectors.counting()))
            .forEach((warehouse, count) -> System.out.printf(" - %-20s : %d available units\n", warehouse, count));
    }

    /**
     * 7. Engineer workload
     */
    /**
     * 7. Engineer workload
     */
    public void printEngineerWorkload() {
        System.out.println("\n--- [STREAM] Engineer Workload ---");
        List<Map<String, Object>> engineers = new ArrayList<>();
        String query = "SELECT engineer_name, service_area, active_tasks FROM field_engineer";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("name", rs.getString("engineer_name"));
                map.put("area", rs.getString("service_area"));
                map.put("tasks", rs.getInt("active_tasks"));
                engineers.add(map);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching engineer data: " + e.getMessage());
        }

        // Explicitly type the lambda parameter as (Map<String, Object> e) to resolve type inference
        engineers.stream()
            .sorted(Comparator.comparingInt((Map<String, Object> e) -> (Integer) e.get("tasks")).reversed())
            .forEach((Map<String, Object> e) -> System.out.printf(" - %-18s (%s) : %d active tasks\n", 
                e.get("name"), e.get("area"), e.get("tasks")));
    }

    /**
     * 8. Failed provisioning requests
     */
    /**
     * 8. Failed provisioning requests
     */
    public void printFailedProvisioningRequests() {
        System.out.println("\n--- [STREAM] Failed Provisioning Requests ---");
        List<Map<String, Object>> requests = new ArrayList<>();
        String query = "SELECT request_id, order_id, service_type, provisioning_status FROM provisioning_request";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("requestId", rs.getInt("request_id"));
                map.put("orderId", rs.getInt("order_id"));
                map.put("type", rs.getString("service_type"));
                map.put("status", rs.getString("provisioning_status"));
                requests.add(map);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching provisioning requests: " + e.getMessage());
        }

        List<Map<String, Object>> failed = requests.stream()
            .filter((Map<String, Object> r) -> "FAILED".equalsIgnoreCase((String) r.get("status")))
            .collect(Collectors.toList());

        if (failed.isEmpty()) {
            System.out.println(" - None! All provisioning requests succeeded.");
        } else {
            failed.forEach((Map<String, Object> f) -> System.out.printf(" - Request #%s (Order #%s, Type: %s) -> FAILED\n",
                f.get("requestId"), f.get("orderId"), f.get("type")));
        }
    }

    /**
     * 9. Average order processing time
     */
    public void printAverageOrderProcessingTime() {
        System.out.println("\n--- [STREAM] Average Order Processing Time ---");
        List<RawOrderItem> orders = fetchAllOrderData();

        OptionalDouble avg = orders.stream()
            .mapToLong(item -> item.processingTimeSeconds)
            .average();

        if (avg.isPresent()) {
            System.out.printf(" - Average Processing Time: %.2f seconds\n", avg.getAsDouble());
        } else {
            System.out.println(" - No orders available to calculate average.");
        }
    }

    /**
     * 10. Top customers by revenue
     */
    public void printTopCustomersByRevenue(int topN) {
        System.out.println("\n--- [STREAM] Top Customers by Revenue (Top " + topN + ") ---");
        List<RawOrderItem> orders = fetchAllOrderData();

        orders.stream()
            .collect(Collectors.groupingBy(
                item -> item.customerName,
                Collectors.summingDouble(item -> item.amount)
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(topN)
            .forEach(entry -> System.out.printf(" - %-20s : $%.2f spent\n", entry.getKey(), entry.getValue()));
    }

    /**
     * Master method to execute all 10 reports at once
     */
    public void runFullStreamAnalytics() {
        System.out.println("\n==================================================");
        System.out.println("        JAVA 8 STREAM API ANALYTICS REPORT        ");
        System.out.println("==================================================");
        printMostOrderedProducts(3);
        printRevenueByProduct();
        printOrdersByStatus();
        printOrdersByCustomerType();
        printInventoryUtilization();
        printAvailableInventoryByWarehouse();
        printEngineerWorkload();
        printFailedProvisioningRequests();
        printAverageOrderProcessingTime();
        printTopCustomersByRevenue(3);
        System.out.println("==================================================\n");
    }
}