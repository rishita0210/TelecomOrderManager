package com.amdocs.telecom.model;

public class TelecomProduct {
    private int productId;
    private String productCode;
    private String productName;
    private String productType;
    private String description;
    private double monthlyPrice;
    private double activationFee;
    private int contractPeriod;

    public TelecomProduct() {}

    public TelecomProduct(String productCode, String productName, String productType, double monthlyPrice) {
        this.productCode = productCode;
        this.productName = productName;
        this.productType = productType;
        this.monthlyPrice = monthlyPrice;
    }

    // Getters and Setters
    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getMonthlyPrice() { return monthlyPrice; }
    public void setMonthlyPrice(double monthlyPrice) { this.monthlyPrice = monthlyPrice; }

    public double getActivationFee() { return activationFee; }
    public void setActivationFee(double activationFee) { this.activationFee = activationFee; }

    public int getContractPeriod() { return contractPeriod; }
    public void setContractPeriod(int contractPeriod) { this.contractPeriod = contractPeriod; }

    @Override
    public String toString() {
        return "Product [ID=" + productId + ", Code=" + productCode + ", Name=" + productName + ", Price=" + monthlyPrice + "]";
    }
}
