package com.amdocs.telecom.dao;

import com.amdocs.telecom.model.TelecomProduct;
import com.amdocs.telecom.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {
    
    public List<TelecomProduct> getAllProducts() {
        List<TelecomProduct> products = new ArrayList<>();
        String query = "SELECT * FROM telecom_product";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                TelecomProduct product = new TelecomProduct();
                product.setProductId(rs.getInt("product_id"));
                product.setProductCode(rs.getString("product_code"));
                product.setProductName(rs.getString("product_name"));
                product.setProductType(rs.getString("product_type"));
                product.setDescription(rs.getString("description"));
                product.setMonthlyPrice(rs.getDouble("monthly_price"));
                product.setActivationFee(rs.getDouble("activation_fee"));
                product.setContractPeriod(rs.getInt("contract_period"));

                products.add(product);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching products: " + e.getMessage());
        }
        return products;
    }
}
