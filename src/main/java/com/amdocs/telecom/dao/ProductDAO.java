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

    public TelecomProduct getProductByCode(String productCode) {
    String query = "SELECT * FROM telecom_product WHERE product_code = ?";
    try (Connection conn = DBConnection.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(query)) {
        pstmt.setString(1, productCode);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            TelecomProduct p = new TelecomProduct();
            p.setProductId(rs.getInt("product_id"));
            p.setProductCode(rs.getString("product_code"));
            p.setProductName(rs.getString("product_name"));
            p.setProductType(rs.getString("product_type"));
            p.setMonthlyPrice(rs.getDouble("monthly_price"));
            return p;
        }
    } catch (SQLException e) {
        System.err.println("Error fetching product by code: " + e.getMessage());
    }
    return null;
}
}
