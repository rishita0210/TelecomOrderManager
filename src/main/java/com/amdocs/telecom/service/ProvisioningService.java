package com.amdocs.telecom.service;

import com.amdocs.telecom.dao.ProvisioningDAO;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProvisioningService {
    // Thread pool to handle background network provisioning tasks
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);
    private final ProvisioningDAO provisioningDAO = new ProvisioningDAO();

    public void triggerAsyncProvisioning(long orderId) {
        executorService.submit(() -> {
            try {
                // Simulate network configuration latency (4 seconds)
                Thread.sleep(4000);

                boolean success = provisioningDAO.completeProvisioning(orderId);
                if (success) {
                    System.out.printf("\n[ASYNC WORKER] Background activation complete for Order #%d! Status updated to ACTIVATED.\n", orderId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("[ASYNC WORKER] Provisioning interrupted: " + e.getMessage());
            }
        });
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
