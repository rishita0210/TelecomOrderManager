package com.amdocs.telecom.model;

public class InventoryItem {
    private int inventoryId;
    private String itemCode;
    private String itemType;
    private String serialNumber;
    private String warehouse;
    private String location;
    private String status;
    private Integer assignedOrderId;

    public InventoryItem() {}

    // Getters and Setters
    public int getInventoryId() { return inventoryId; }
    public void setInventoryId(int inventoryId) { this.inventoryId = inventoryId; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getWarehouse() { return warehouse; }
    public void setWarehouse(String warehouse) { this.warehouse = warehouse; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getAssignedOrderId() { return assignedOrderId; }
    public void setAssignedOrderId(Integer assignedOrderId) { this.assignedOrderId = assignedOrderId; }

    @Override
    public String toString() {
        return "Inventory [ID=" + inventoryId + ", Type=" + itemType + ", Serial=" + serialNumber + ", Status=" + status + "]";
    }
}
