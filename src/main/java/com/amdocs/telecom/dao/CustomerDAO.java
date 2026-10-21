package com.amdocs.telecom.dao;

import com.amdocs.telecom.model.Customer;
import com.amdocs.telecom.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CustomerDAO {

    // 1. Method to insert a new customer into the database
    public boolean addCustomer(Customer customer) {
        String query = "INSERT INTO customer (customer_number, customer_name, email, mobile_number, customer_type, address, city) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
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

    // 2. Method to retrieve a customer by email for login & authentication
    public Customer getCustomerByEmail(String email) {
        String query = "SELECT * FROM customer WHERE email = ?";
        Customer customer = null;
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                customer = new Customer();
                customer.setCustomerId(rs.getInt("customer_id"));
                customer.setCustomerNumber(rs.getString("customer_number"));
                customer.setCustomerName(rs.getString("customer_name"));
                customer.setEmail(rs.getString("email"));
                customer.setMobileNumber(rs.getString("mobile_number"));
                customer.setCustomerType(rs.getString("customer_type"));
                customer.setAddress(rs.getString("address"));
                customer.setCity(rs.getString("city"));
                customer.setIdentityStatus(rs.getString("identity_status"));
                customer.setAccountStatus(rs.getString("account_status"));
                customer.setRegistrationDate(rs.getTimestamp("registration_date"));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching customer by email: " + e.getMessage());
        }
        
        return customer;
    }
}