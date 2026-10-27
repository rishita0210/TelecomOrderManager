package com.amdocs.telecom.model;

import java.sql.Timestamp;

public class ProvisioningRequest {
    private int provisioningId;
    private int orderId;
    private int serviceId;
    private String provisioningType;
    private String networkElement;
    private Timestamp requestedDate;
    private Timestamp completedDate;
    private String status;
    private String errorMessage;

    public ProvisioningRequest() {}

    public ProvisioningRequest(int orderId, int serviceId, String provisioningType, String networkElement) {
        this.orderId = orderId;
        this.serviceId = serviceId;
        this.provisioningType = provisioningType;
        this.networkElement = networkElement;
        this.status = "PENDING";
        this.requestedDate = new Timestamp(System.currentTimeMillis());
    }

    public int getProvisioningId() { return provisioningId; }
    public void setProvisioningId(int provisioningId) { this.provisioningId = provisioningId; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public int getServiceId() { return serviceId; }
    public void setServiceId(int serviceId) { this.serviceId = serviceId; }

    public String getProvisioningType() { return provisioningType; }
    public void setProvisioningType(String provisioningType) { this.provisioningType = provisioningType; }

    public String getNetworkElement() { return networkElement; }
    public void setNetworkElement(String networkElement) { this.networkElement = networkElement; }

    public Timestamp getRequestedDate() { return requestedDate; }
    public void setRequestedDate(Timestamp requestedDate) { this.requestedDate = requestedDate; }

    public Timestamp getCompletedDate() { return completedDate; }
    public void setCompletedDate(Timestamp completedDate) { this.completedDate = completedDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    @Override
    public String toString() {
        return "ProvisioningRequest [ID=" + provisioningId + ", OrderID=" + orderId +
               ", Type=" + provisioningType + ", Node=" + networkElement +
               ", Status=" + status + ", Completed=" + completedDate + "]";
    }
}