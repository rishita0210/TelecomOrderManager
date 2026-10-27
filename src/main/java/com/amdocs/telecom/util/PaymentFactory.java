package com.amdocs.telecom.util;

public class PaymentFactory {
    public static PaymentProcessor getProcessor(String paymentMode) {
        if (paymentMode == null) return null;
        switch (paymentMode.trim().toUpperCase()) {
            case "UPI":
                return new UpiPaymentProcessor();
            case "CARD":
                return new CardPaymentProcessor();
            case "NET_BANKING":
                return new NetBankingPaymentProcessor();
            case "BANK_TRANSFER":
                return new BankTransferPaymentProcessor();
            default:
                return null;
        }
    }
}