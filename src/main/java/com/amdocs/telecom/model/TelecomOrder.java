package com.amdocs.telecom.model;

import java.sql.Timestamp;

public class TelecomOrder {
    private int orderId;
    private String orderNumber;
    private int customerId;
    private String orderType; // NEW_CONNECTION, BROADBAND, ESIM_ACTIVATION, etc.
    private String orderStatus; // CONFIRMED, ACTIVATED, CANCELLED
    private double totalAmount;
    private Timestamp requestedActivationDate;
    private Timestamp orderDate;

    public TelecomOrder() {}

    public TelecomOrder(String orderNumber, int customerId, String orderType, 
                        String orderStatus, double totalAmount) {
        this.orderNumber = orderNumber;
        this.customerId = customerId;
        this.orderType = orderType;
        this.orderStatus = orderStatus;
        this.totalAmount = totalAmount;
    }

    // Getters and Setters
    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public Timestamp getRequestedActivationDate() { return requestedActivationDate; }
    public void setRequestedActivationDate(Timestamp requestedActivationDate) { 
        this.requestedActivationDate = requestedActivationDate; 
    }

    public Timestamp getOrderDate() { return orderDate; }
    public void setOrderDate(Timestamp orderDate) { this.orderDate = orderDate; }

    @Override
    public String toString() {
        return "TelecomOrder [ID=" + orderId + ", Order#=" + orderNumber + ", Type=" + orderType + 
               ", Status=" + orderStatus + ", Amount=$" + totalAmount + "]";
    }
}