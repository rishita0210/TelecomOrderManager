package com.amdocs.telecom.dao;

import com.amdocs.telecom.model.FieldEngineer;
import com.amdocs.telecom.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class EngineerDAO {

    // 1. Fetch all engineers for an area from MySQL
    public List<FieldEngineer> getEngineersByArea(String area) {
        List<FieldEngineer> list = new ArrayList<>();
        String query = "SELECT * FROM field_engineer WHERE service_area = ? AND status = 'ACTIVE'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, area);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                list.add(new FieldEngineer(
                    rs.getInt("engineer_id"),
                    rs.getString("engineer_name"),
                    rs.getString("service_area"),
                    rs.getInt("active_tasks"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching engineers: " + e.getMessage());
        }
        return list;
    }

    // 2. Demonstration of Java 8 Streams: Find the engineer with the least tasks
    public Optional<FieldEngineer> assignLeastBusyEngineer(String area) {
        List<FieldEngineer> engineers = getEngineersByArea(area);

        // Using Java 8 Stream API: filter and min with Comparator
        Optional<FieldEngineer> bestEngineer = engineers.stream()
            .filter(e -> "ACTIVE".equalsIgnoreCase(e.getStatus()))
            .min(Comparator.comparingInt(FieldEngineer::getActiveTasks));

        // If an engineer was found, increment their active_tasks count in the database
        bestEngineer.ifPresent(engineer -> incrementTaskCount(engineer.getEngineerId()));

        return bestEngineer;
    }

    private void incrementTaskCount(int engineerId) {
        String updateQuery = "UPDATE field_engineer SET active_tasks = active_tasks + 1 WHERE engineer_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
            pstmt.setInt(1, engineerId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating engineer workload: " + e.getMessage());
        }
    }
}