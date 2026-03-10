package com.example.financial.autopay.repository;

import com.example.financial.autopay.model.entity.AutoPayExecutionLog;
import com.example.financial.autopay.model.entity.ExecutionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AutoPayExecutionLogRepository extends JpaRepository<AutoPayExecutionLog, UUID> {

  /** Paginated logs for all of a user's schedules. */
  Page<AutoPayExecutionLog> findByUserIdOrderByExecutionDateDesc(UUID userId, Pageable pageable);

  /** Paginated logs for a specific schedule (ownership validated via join). */
  @Query("""
      SELECT l FROM AutoPayExecutionLog l
      WHERE l.schedule.id = :scheduleId
        AND l.user.id = :userId
      ORDER BY l.executionDate DESC
      """)
  Page<AutoPayExecutionLog> findByScheduleAndUser(
      @Param("scheduleId") UUID scheduleId,
      @Param("userId") UUID userId,
      Pageable pageable);

  /** Count successful payments for health score calculation. */
  long countByUserIdAndStatus(UUID userId, ExecutionStatus status);

  /** Recent execution logs for a schedule (used for AI insights). */
  List<AutoPayExecutionLog> findTop10ByScheduleIdOrderByExecutionDateDesc(UUID scheduleId);

  // ── Plaid verification queries ────────────────────────────────────────────

  /**
   * Finds UNVERIFIED logs that were executed before the given cutoff date.
   * Used by the nightly PlaidVerificationJob to attempt verification or
   * escalate to NEEDS_REVIEW.
   */
  @Query("""
      SELECT l FROM AutoPayExecutionLog l
      WHERE l.plaidVerificationStatus = 'UNVERIFIED'
        AND l.status = 'SUCCESS'
        AND l.executionDate <= :cutoff
      ORDER BY l.executionDate ASC
      """)
  List<AutoPayExecutionLog> findUnverifiedSuccessfulBefore(
      @Param("cutoff") java.time.OffsetDateTime cutoff);

  /**
   * Finds all SUCCESS logs for a user, not yet verified, executed within a window
   * —
   * used for single-schedule verification upon Plaid sync.
   */
  @Query("""
      SELECT l FROM AutoPayExecutionLog l
      WHERE l.user.id = :userId
        AND l.status = 'SUCCESS'
        AND l.plaidVerificationStatus = 'UNVERIFIED'
        AND l.executionDate >= :after
      ORDER BY l.executionDate DESC
      """)
  List<AutoPayExecutionLog> findUnverifiedForUser(
      @Param("userId") UUID userId,
      @Param("after") java.time.OffsetDateTime after);

  /**
   * Finds failed logs eligible for retry.
   * i.e., status = FAILED, retryCount < maxRetries, and executionDate is older
   * than cutoff.
   */
  @Query("""
      SELECT l FROM AutoPayExecutionLog l
      WHERE l.status = 'FAILED'
        AND l.retryCount < :maxRetries
        AND l.executionDate <= :cutoff
      ORDER BY l.executionDate ASC
      """)
  List<AutoPayExecutionLog> findEligibleForRetry(
      @Param("maxRetries") int maxRetries,
      @Param("cutoff") java.time.OffsetDateTime cutoff);

  /** Find execution log by Stripe PaymentIntent ID for webhook reconciliation. */
  java.util.Optional<AutoPayExecutionLog> findByStripePaymentIntentId(String stripePaymentIntentId);
}
