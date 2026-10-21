package com.amdocs.telecom.model;

public class FieldEngineer {
    private int engineerId;
    private String engineerName;
    private String serviceArea;
    private int activeTasks;
    private String status;

    public FieldEngineer() {}

    public FieldEngineer(int engineerId, String engineerName, String serviceArea, int activeTasks, String status) {
        this.engineerId = engineerId;
        this.engineerName = engineerName;
        this.serviceArea = serviceArea;
        this.activeTasks = activeTasks;
        this.status = status;
    }

    public int getEngineerId() { return engineerId; }
    public void setEngineerId(int engineerId) { this.engineerId = engineerId; }

    public String getEngineerName() { return engineerName; }
    public void setEngineerName(String engineerName) { this.engineerName = engineerName; }

    public String getServiceArea() { return serviceArea; }
    public void setServiceArea(String serviceArea) { this.serviceArea = serviceArea; }

    public int getActiveTasks() { return activeTasks; }
    public void setActiveTasks(int activeTasks) { this.activeTasks = activeTasks; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("Engineer #%d: %s | Area: %s | Active Tasks: %d", 
            engineerId, engineerName, serviceArea, activeTasks);
    }
}
