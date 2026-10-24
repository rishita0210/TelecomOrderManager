package com.amdocs.telecom.model;

public class FieldEngineer {
    private int engineerId;
    private String employeeCode;
    private String engineerName;
    private String specialization;
    private String serviceArea;
    private int activeTasks;
    private int experienceYears;
    private String status;

    public FieldEngineer() {}

    public FieldEngineer(int engineerId, String employeeCode, String engineerName, 
                         String specialization, String serviceArea, int activeTasks, 
                         int experienceYears, String status) {
        this.engineerId = engineerId;
        this.employeeCode = employeeCode;
        this.engineerName = engineerName;
        this.specialization = specialization;
        this.serviceArea = serviceArea;
        this.activeTasks = activeTasks;
        this.experienceYears = experienceYears;
        this.status = status;
    }

    public int getEngineerId() { return engineerId; }
    public void setEngineerId(int engineerId) { this.engineerId = engineerId; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getEngineerName() { return engineerName; }
    public void setEngineerName(String engineerName) { this.engineerName = engineerName; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getServiceArea() { return serviceArea; }
    public void setServiceArea(String serviceArea) { this.serviceArea = serviceArea; }

    public int getActiveTasks() { return activeTasks; }
    public void setActiveTasks(int activeTasks) { this.activeTasks = activeTasks; }

    public int getExperienceYears() { return experienceYears; }
    public void setExperienceYears(int experienceYears) { this.experienceYears = experienceYears; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("[%s] %s | Skill: %s | Area: %s | Tasks: %d | Exp: %d yrs",
            employeeCode, engineerName, specialization, serviceArea, activeTasks, experienceYears);
    }
}