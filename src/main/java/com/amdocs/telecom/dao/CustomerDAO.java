package com.amdocs.telecom.dao;

import com.amdocs.telecom.model.Customer;
import com.amdocs.telecom.util.DBConnection;

import java.sql.*;

public class CustomerDAO {

    public boolean addCustomer(Customer customer) {
        String query = "INSERT INTO customer (customer_number, customer_name, email, password, mobile_number, customer_type, address, city) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, customer.getCustomerNumber());
            pstmt.setString(2, customer.getCustomerName());
            pstmt.setString(3, customer.getEmail());
            pstmt.setString(4, customer.getPassword());
            pstmt.setString(5, customer.getMobileNumber());
            pstmt.setString(6, customer.getCustomerType());
            pstmt.setString(7, customer.getAddress());
            pstmt.setString(8, customer.getCity());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error adding customer: " + e.getMessage());
            return false;
        }
    }

    public Customer getCustomerByEmail(String email) {
        String query = "SELECT * FROM customer WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Customer c = new Customer();
                c.setCustomerId(rs.getInt("customer_id"));
                c.setCustomerNumber(rs.getString("customer_number"));
                c.setCustomerName(rs.getString("customer_name"));
                c.setEmail(rs.getString("email"));
                c.setPassword(rs.getString("password"));
                c.setMobileNumber(rs.getString("mobile_number"));
                c.setCustomerType(rs.getString("customer_type"));
                c.setAddress(rs.getString("address"));
                c.setCity(rs.getString("city"));
                c.setIdentityStatus(rs.getString("identity_status"));
                c.setAccountStatus(rs.getString("account_status"));
                c.setRegistrationDate(rs.getTimestamp("registration_date"));
                return c;
            }
        } catch (SQLException e) {
            System.err.println("Error fetching customer by email: " + e.getMessage());
        }
        return null;
    }

    public boolean updatePassword(String email, String newPassword) {
        String query = "UPDATE customer SET password = ? WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, newPassword);
            pstmt.setString(2, email);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error resetting password: " + e.getMessage());
            return false;
        }
    }
}