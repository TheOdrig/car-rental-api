package com.akif.damage.internal.repository;

import com.akif.damage.api.DamageCategory;
import com.akif.damage.api.DamageSeverity;
import com.akif.damage.api.DamageStatus;
import com.akif.damage.domain.model.DamageReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface DamageReportRepository extends JpaRepository<DamageReport, Long> {

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true)
    @Query("UPDATE DamageReport d SET d.customerFullName = :name WHERE d.customerUserId = :userId")
    int refreshCustomerName(@Param("userId") Long userId, @Param("name") String name);

        Optional<DamageReport> findByIdAndIsDeletedFalse(Long id);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT d FROM DamageReport d WHERE d.id = :id AND d.isDeleted = false")
        Optional<DamageReport> findByIdForUpdate(@Param("id") Long id);

        Page<DamageReport> findByRentalIdAndIsDeletedFalse(Long rentalId, Pageable pageable);

        List<DamageReport> findByRentalIdAndIsDeletedFalse(Long rentalId);

        Page<DamageReport> findByCarIdAndIsDeletedFalse(Long carId, Pageable pageable);

        Page<DamageReport> findByCustomerUserIdAndIsDeletedFalse(Long customerUserId, Pageable pageable);

        Page<DamageReport> findByStatusAndIsDeletedFalse(DamageStatus status, Pageable pageable);

        Optional<DamageReport> findByPaymentId(Long paymentId);

        @Query("""
                        SELECT COUNT(d) > 0 FROM DamageReport d
                        WHERE d.rentalId = :rentalId
                        AND d.isDeleted = false
                        AND d.status NOT IN ('CLOSED', 'WAIVED')
                        """)
        boolean existsPendingByRentalId(@Param("rentalId") Long rentalId);

        @Query("""
                        SELECT COUNT(d) > 0 FROM DamageReport d
                        WHERE d.carId = :carId
                        AND d.isDeleted = false
                        AND d.status NOT IN ('CLOSED', 'WAIVED')
                        """)
        boolean existsPendingByCarId(@Param("carId") Long carId);

        @Query("""
                        SELECT d FROM DamageReport d
                        WHERE d.rentalId = :rentalId
                        AND d.isDeleted = false
                        AND d.status NOT IN ('CLOSED', 'WAIVED')
                        """)
        List<DamageReport> findPendingByRentalId(@Param("rentalId") Long rentalId);

        @Query("""
                        SELECT d FROM DamageReport d WHERE
                        d.isDeleted = false
                        AND (CAST(:startDate AS java.time.LocalDate) IS NULL OR CAST(d.reportedAt AS date) >= :startDate)
                        AND (CAST(:endDate AS java.time.LocalDate) IS NULL OR CAST(d.reportedAt AS date) <= :endDate)
                        AND (:severity IS NULL OR d.severity = :severity)
                        AND (:category IS NULL OR d.category = :category)
                        AND (:status IS NULL OR d.status = :status)
                        AND (:carId IS NULL OR d.carId = :carId)
                        """)
        Page<DamageReport> searchDamages(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate,
                        @Param("severity") DamageSeverity severity,
                        @Param("category") DamageCategory category,
                        @Param("status") DamageStatus status,
                        @Param("carId") Long carId,
                        Pageable pageable);

        @Query("""
                        SELECT COUNT(d) FROM DamageReport d WHERE
                        d.isDeleted = false
                        AND (CAST(:startDate AS java.time.LocalDate) IS NULL OR CAST(d.reportedAt AS date) >= :startDate)
                        AND (CAST(:endDate AS java.time.LocalDate) IS NULL OR CAST(d.reportedAt AS date) <= :endDate)
                        """)
        int countTotalDamages(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("""
                        SELECT COUNT(d) FROM DamageReport d WHERE
                        d.isDeleted = false
                        AND d.severity = :severity
                        AND (CAST(:startDate AS java.time.LocalDate) IS NULL OR CAST(d.reportedAt AS date) >= :startDate)
                        AND (CAST(:endDate AS java.time.LocalDate) IS NULL OR CAST(d.reportedAt AS date) <= :endDate)
                        """)
        int countBySeverity(
                        @Param("severity") DamageSeverity severity,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("""
                        SELECT COUNT(d) FROM DamageReport d WHERE
                        d.isDeleted = false
                        AND d.status = :status
                        AND (CAST(:startDate AS java.time.LocalDate) IS NULL OR CAST(d.reportedAt AS date) >= :startDate)
                        AND (CAST(:endDate AS java.time.LocalDate) IS NULL OR CAST(d.reportedAt AS date) <= :endDate)
                        """)
        int countByStatus(
                        @Param("status") DamageStatus status,
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("""
                        SELECT COALESCE(AVG(d.repairCostEstimate), 0) FROM DamageReport d WHERE
                        d.isDeleted = false
                        AND (CAST(:startDate AS java.time.LocalDate) IS NULL OR CAST(d.reportedAt AS date) >= :startDate)
                        AND (CAST(:endDate AS java.time.LocalDate) IS NULL OR CAST(d.reportedAt AS date) <= :endDate)
                        """)
        BigDecimal averageRepairCost(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("""
                        SELECT d.currency, COUNT(d), COALESCE(SUM(d.repairCostEstimate), 0),
                        COALESCE(SUM(d.customerLiability), 0), COUNT(d.repairCostEstimate)
                        FROM DamageReport d WHERE
                        d.isDeleted = false
                        AND d.currency IS NOT NULL
                        AND (CAST(:startDate AS java.time.LocalDate) IS NULL OR CAST(d.reportedAt AS date) >= :startDate)
                        AND (CAST(:endDate AS java.time.LocalDate) IS NULL OR CAST(d.reportedAt AS date) <= :endDate)
                        GROUP BY d.currency
                        ORDER BY d.currency
                        """)
        List<Object[]> totalsByCurrency(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("""
                        SELECT COUNT(d) FROM DamageReport d
                        WHERE d.isDeleted = false
                        AND d.status IN (com.akif.damage.api.DamageStatus.REPORTED, com.akif.damage.api.DamageStatus.UNDER_ASSESSMENT)
                        """)
        int countPendingAssessments();

        @Query("""
                        SELECT COUNT(d) FROM DamageReport d
                        WHERE d.isDeleted = false
                        AND d.status = com.akif.damage.api.DamageStatus.DISPUTED
                        AND d.reportedAt < :cutoffDate
                        """)
        int countUnresolvedDisputesOlderThan(@Param("cutoffDate") Instant cutoffDate);

        int countByCustomerUserIdAndIsDeletedFalse(Long customerUserId);

        @Query("""
                        SELECT d.currency, COUNT(d), COALESCE(SUM(d.customerLiability), 0)
                        FROM DamageReport d
                        WHERE d.customerUserId = :customerUserId
                        AND d.isDeleted = false
                        AND d.currency IS NOT NULL
                        GROUP BY d.currency
                        ORDER BY d.currency
                        """)
        List<Object[]> customerLiabilityByCurrency(@Param("customerUserId") Long customerUserId);
}