package com.amdocs.telecom.service;

import com.amdocs.telecom.exception.*;
import com.amdocs.telecom.model.Customer;
import com.amdocs.telecom.model.TelecomProduct;
import com.amdocs.telecom.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class OrderValidationService {

    public void validateOrder(Customer customer, TelecomProduct product, String paymentMode) 
            throws CustomerNotEligibleException, ProductUnavailableException, 
                   InvalidOrderException, DuplicateOrderException {

        // 1. Customer Status & Identity Check
        if (customer == null) {
            throw new CustomerNotEligibleException("Customer does not exist in the system.");
        }
        if (!"ACTIVE".equalsIgnoreCase(customer.getAccountStatus())) {
            throw new CustomerNotEligibleException("Customer account is " + customer.getAccountStatus() + ". Orders cannot be placed.");
        }
        if (!"VERIFIED".equalsIgnoreCase(customer.getIdentityStatus())) {
            throw new CustomerNotEligibleException("KYC / Required documents not verified. Please submit identification.");
        }

        // 2. Product Availability Check
        if (product == null) {
            throw new ProductUnavailableException("Selected product is unavailable or not found in catalog.");
        }

        // 3. Contract Restrictions (e.g. Individual accounts limited to max 5 active subscriptions)
        if ("INDIVIDUAL".equalsIgnoreCase(customer.getCustomerType())) {
            int activeLines = getCustomerActiveLineCount(customer.getCustomerId());
            if (activeLines >= 5) {
                throw new CustomerNotEligibleException("Contract Limit Reached: Individual accounts cannot exceed 5 active lines.");
            }
        }

        // 4. Duplicate Order Check (Prevent double submissions within last 60 seconds)
        if (hasRecentPendingOrder(customer.getCustomerId(), product.getProductId())) {
            throw new DuplicateOrderException("A pending order for this product was already placed recently. Please wait.");
        }

        // 5. Payment Mode Validation
        if (!"UPI".equalsIgnoreCase(paymentMode) && !"CARD".equalsIgnoreCase(paymentMode) && !"NET_BANKING".equalsIgnoreCase(paymentMode)) {
            throw new InvalidOrderException("Invalid payment method: " + paymentMode + ". Supported: UPI, CARD, NET_BANKING.");
        }

        System.out.println("[✔] Order Validation Passed: Customer status, documents, eligibility, and product verified.");
    }

    private int getCustomerActiveLineCount(int customerId) {
        String sql = "SELECT COUNT(*) FROM telecom_order WHERE customer_id = ? AND order_status IN ('CONFIRMED', 'ACTIVATED')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Eligibility lookup error: " + e.getMessage());
        }
        return 0;
    }

    private boolean hasRecentPendingOrder(int customerId, int productId) {
        String sql = "SELECT COUNT(*) FROM telecom_order o " +
                     "JOIN order_item oi ON o.order_id = oi.order_id " +
                     "WHERE o.customer_id = ? AND oi.product_id = ? AND o.order_status = 'CONFIRMED' " +
                     "AND o.order_date >= NOW() - INTERVAL 1 MINUTE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            pstmt.setInt(2, productId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("Duplicate check error: " + e.getMessage());
        }
        return false;
    }
}
