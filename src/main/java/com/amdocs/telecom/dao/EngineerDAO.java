package com.amdocs.telecom.dao;

import com.amdocs.telecom.model.FieldEngineer;
import com.amdocs.telecom.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class EngineerDAO {

    /**
     * Retrieves all active engineers located in a specific service area.
     */
    public List<FieldEngineer> getEngineersByArea(String area) {
        List<FieldEngineer> list = new ArrayList<>();
        String query = "SELECT * FROM field_engineer WHERE service_area = ? AND status = 'ACTIVE'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, area);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    FieldEngineer eng = new FieldEngineer();
                    eng.setEngineerId(rs.getInt("engineer_id"));
                    eng.setEngineerName(rs.getString("engineer_name"));
                    eng.setServiceArea(rs.getString("service_area"));
                    eng.setActiveTasks(rs.getInt("active_tasks"));
                    eng.setStatus(rs.getString("status"));
                    list.add(eng);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching engineers by area: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves all active engineers across all regions.
     */
    public List<FieldEngineer> getAllActiveEngineers() {
        List<FieldEngineer> list = new ArrayList<>();
        String query = "SELECT * FROM field_engineer WHERE status = 'ACTIVE'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                FieldEngineer eng = new FieldEngineer();
                eng.setEngineerId(rs.getInt("engineer_id"));
                eng.setEngineerName(rs.getString("engineer_name"));
                eng.setServiceArea(rs.getString("service_area"));
                eng.setActiveTasks(rs.getInt("active_tasks"));
                eng.setStatus(rs.getString("status"));
                list.add(eng);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all active engineers: " + e.getMessage());
        }
        return list;
    }

    /**
     * Workload balancing using Java 8 Streams:
     * Filters active technicians in the area and selects the one with the lowest task count.
     * (Called directly by MainApp.java)
     */
    public Optional<FieldEngineer> assignLeastBusyEngineer(String area) {
        List<FieldEngineer> engineers = getEngineersByArea(area);

        // Java 8 Stream Pipeline: Predicate filter + Min Comparator
        Optional<FieldEngineer> bestEngineer = engineers.stream()
            .filter(e -> "ACTIVE".equalsIgnoreCase(e.getStatus()))
            .min(Comparator.comparingInt(FieldEngineer::getActiveTasks));

        // If an engineer is identified, increment their workload in MySQL
        bestEngineer.ifPresent(eng -> incrementActiveTasks(eng.getEngineerId()));

        return bestEngineer;
    }

    /**
     * Advanced Section 11 matching using Stream, Lambda, Comparator, and Optional:
     * Evaluates availability, region, and lowest active workload.
     */
    public Optional<FieldEngineer> recommendEngineer(String requiredSkill, String region) {
        List<FieldEngineer> engineers = getAllActiveEngineers();

        Optional<FieldEngineer> recommended = engineers.stream()
            // 1. Availability check via Lambda predicate
            .filter(e -> "ACTIVE".equalsIgnoreCase(e.getStatus()))
            // 2. Region check via Lambda predicate
            .filter(e -> e.getServiceArea().equalsIgnoreCase(region))
            // 3. Comparator: selects minimum workload
            .min(Comparator.comparingInt(FieldEngineer::getActiveTasks));

        // If an engineer matches, increment their task count
        recommended.ifPresent(eng -> incrementActiveTasks(eng.getEngineerId()));

        return recommended;
    }

    /**
     * Increments the active_tasks count for an assigned engineer in MySQL.
     */
    public void incrementActiveTasks(int engineerId) {
        String updateQuery = "UPDATE field_engineer SET active_tasks = active_tasks + 1 WHERE engineer_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {

            pstmt.setInt(1, engineerId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating technician workload: " + e.getMessage());
        }
    }
}