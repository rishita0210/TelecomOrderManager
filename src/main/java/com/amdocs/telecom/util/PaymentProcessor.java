package com.amdocs.telecom.util;

public interface PaymentProcessor {
    boolean process(double amount);
}

class UpiPaymentProcessor implements PaymentProcessor {
    @Override
    public boolean process(double amount) {
        System.out.printf("[PAYMENT GATEWAY - UPI] Verifying VPA and transferring $%.2f... SUCCESS.\n", amount);
        return true;
    }
}

class CardPaymentProcessor implements PaymentProcessor {
    @Override
    public boolean process(double amount) {
        System.out.printf("[PAYMENT GATEWAY - CARD] Authorizing merchant charge of $%.2f... SUCCESS.\n", amount);
        return true;
    }
}

class NetBankingPaymentProcessor implements PaymentProcessor {
    @Override
    public boolean process(double amount) {
        System.out.printf("[PAYMENT GATEWAY - NET_BANKING] Secure banking session debited $%.2f... SUCCESS.\n", amount);
        return true;
    }
}
