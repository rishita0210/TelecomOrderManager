package com.amdocs.telecom.dao;

import com.amdocs.telecom.model.Customer;
import com.amdocs.telecom.util.DBConnection;
import java.sql.*;

public class CustomerDAO {
    public boolean addCustomer(Customer customer) {
        String query = "INSERT INTO customer (customer_number, customer_name, email, mobile_number, customer_type, address, city) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, customer.getCustomerNumber());
            pstmt.setString(2, customer.getCustomerName());
            pstmt.setString(3, customer.getEmail());
            pstmt.setString(4, customer.getMobileNumber());
            pstmt.setString(5, customer.getCustomerType());
            pstmt.setString(6, customer.getAddress());
            pstmt.setString(7, customer.getCity());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error adding customer: " + e.getMessage());
            return false;
        }
    }
}
