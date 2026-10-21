package com.amdocs.telecom.model;

import java.sql.Timestamp;

public class Customer {
    private int customerId;
    private String customerNumber;
    private String customerName;
    private String email;
    private String mobileNumber;
    private String customerType;
    private String address;
    private String city;
    private String identityStatus;
    private String accountStatus;
    private Timestamp registrationDate;

    public Customer() {}

    public Customer(String customerNumber, String customerName, String email, 
                    String mobileNumber, String customerType, String address, String city) {
        this.customerNumber = customerNumber;
        this.customerName = customerName;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.customerType = customerType;
        this.address = address;
        this.city = city;
    }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getCustomerNumber() { return customerNumber; }
    public void setCustomerNumber(String customerNumber) { this.customerNumber = customerNumber; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getCustomerType() { return customerType; }
    public void setCustomerType(String customerType) { this.customerType = customerType; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getIdentityStatus() { return identityStatus; }
    public void setIdentityStatus(String identityStatus) { this.identityStatus = identityStatus; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

    public java.sql.Timestamp getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(java.sql.Timestamp registrationDate) { this.registrationDate = registrationDate; }

    @Override
    public String toString() {
        return "Customer [ID=" + customerId + ", Name=" + customerName + ", Email=" + email + "]";
    }
}