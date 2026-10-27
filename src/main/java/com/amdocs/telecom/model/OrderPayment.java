package com.amdocs.telecom.model;

import java.sql.Timestamp;

public class OrderPayment {
    private int paymentId;
    private int orderId;
    private String transactionReference;
    private double amount;
    private String paymentMode;
    private Timestamp paymentDate;
    private String status;

    public OrderPayment() {}

    public OrderPayment(int orderId, String transactionReference, double amount, String paymentMode, String status) {
        this.orderId = orderId;
        this.transactionReference = transactionReference;
        this.amount = amount;
        this.paymentMode = paymentMode;
        this.status = status;
        this.paymentDate = new Timestamp(System.currentTimeMillis());
    }

    public int getPaymentId() { return paymentId; }
    public void setPaymentId(int paymentId) { this.paymentId = paymentId; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public Timestamp getPaymentDate() { return paymentDate; }
    public void setPaymentDate(Timestamp paymentDate) { this.paymentDate = paymentDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "OrderPayment [ID=" + paymentId + ", OrderID=" + orderId + ", TxRef=" + transactionReference +
               ", Amount=$" + amount + ", Mode=" + paymentMode + ", Status=" + status + "]";
    }
}