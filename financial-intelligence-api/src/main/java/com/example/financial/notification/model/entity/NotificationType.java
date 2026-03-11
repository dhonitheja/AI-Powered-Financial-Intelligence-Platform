package com.example.financial.notification.model.entity;

public enum NotificationType {
    PAYMENT_SUCCESS, // AutoPay executed successfully
    PAYMENT_FAILED, // AutoPay execution failed
    PAYMENT_DUE_REMINDER, // Payment due in X days
    PAYMENT_OVERDUE, // Payment is overdue
    PAYMENT_METHOD_EXPIRING, // Card expiring soon
    BANK_CONNECTION_LOST, // Plaid connection needs refresh
    SECURITY_ALERT, // IDOR attempt or suspicious activity
    SYSTEM_MESSAGE // General platform updates
}
