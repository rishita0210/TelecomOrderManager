package com.amdocs.telecom.dao;

import com.amdocs.telecom.model.InventoryItem;
import com.amdocs.telecom.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InventoryDAO {

    public List<InventoryItem> getAvailableInventoryByType(String itemType) {
        List<InventoryItem> items = new ArrayList<>();
        String query = "SELECT * FROM inventory_item WHERE item_type = ? AND status = 'AVAILABLE'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, itemType);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                InventoryItem item = new InventoryItem();
                item.setInventoryId(rs.getInt("inventory_id"));
                item.setItemCode(rs.getString("item_code"));
                item.setItemType(rs.getString("item_type"));
                item.setSerialNumber(rs.getString("serial_number"));
                item.setWarehouse(rs.getString("warehouse"));
                item.setLocation(rs.getString("location"));
                item.setStatus(rs.getString("status"));

                items.add(item);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching inventory: " + e.getMessage());
        }
        return items;
    }
}