package com.example.financial.autopay.model.entity;

/**
 * Execution status for autopay payment logs.
 */
public enum ExecutionStatus {
    PENDING,
    SUCCESS,
    FAILED,
    SKIPPED,
    CANCELLED
}
