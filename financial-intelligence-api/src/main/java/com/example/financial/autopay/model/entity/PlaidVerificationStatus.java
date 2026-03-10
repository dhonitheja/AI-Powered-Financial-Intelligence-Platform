package com.example.financial.autopay.model.entity;

/**
 * Tracks whether an autopay execution has been cross-verified via Plaid.
 *
 * State machine:
 * UNVERIFIED → initial state after Stripe executes
 * VERIFIED → Plaid sync found a matching bank transaction
 * NEEDS_REVIEW → Plaid sync ran 5+ days after execution and found no match
 */
public enum PlaidVerificationStatus {
    UNVERIFIED,
    VERIFIED,
    NEEDS_REVIEW
}
