package com.akif.damage.internal.repository;

import com.akif.damage.domain.enums.DamageCategory;
import com.akif.damage.domain.enums.DamageSeverity;
import com.akif.damage.domain.enums.DamageStatus;
import com.akif.damage.domain.model.DamageReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DamageReportRepository extends JpaRepository<DamageReport, Long> {

    Optional<DamageReport> findByIdAndIsDeletedFalse(Long id);

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
            AND (:startDate IS NULL OR CAST(d.reportedAt AS date) >= :startDate) 
            AND (:endDate IS NULL OR CAST(d.reportedAt AS date) <= :endDate) 
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
            AND (:startDate IS NULL OR CAST(d.reportedAt AS date) >= :startDate) 
            AND (:endDate IS NULL OR CAST(d.reportedAt AS date) <= :endDate)
            """)
    int countTotalDamages(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT COUNT(d) FROM DamageReport d WHERE 
            d.isDeleted = false 
            AND d.severity = :severity 
            AND (:startDate IS NULL OR CAST(d.reportedAt AS date) >= :startDate) 
            AND (:endDate IS NULL OR CAST(d.reportedAt AS date) <= :endDate)
            """)
    int countBySeverity(
            @Param("severity") DamageSeverity severity,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT COUNT(d) FROM DamageReport d WHERE 
            d.isDeleted = false 
            AND d.status = :status 
            AND (:startDate IS NULL OR CAST(d.reportedAt AS date) >= :startDate) 
            AND (:endDate IS NULL OR CAST(d.reportedAt AS date) <= :endDate)
            """)
    int countByStatus(
            @Param("status") DamageStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT COALESCE(SUM(d.repairCostEstimate), 0) FROM DamageReport d WHERE 
            d.isDeleted = false 
            AND (:startDate IS NULL OR CAST(d.reportedAt AS date) >= :startDate) 
            AND (:endDate IS NULL OR CAST(d.reportedAt AS date) <= :endDate)
            """)
    BigDecimal sumTotalRepairCost(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT COALESCE(SUM(d.customerLiability), 0) FROM DamageReport d WHERE 
            d.isDeleted = false 
            AND (:startDate IS NULL OR CAST(d.reportedAt AS date) >= :startDate) 
            AND (:endDate IS NULL OR CAST(d.reportedAt AS date) <= :endDate)
            """)
    BigDecimal sumTotalCustomerLiability(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT COALESCE(AVG(d.repairCostEstimate), 0) FROM DamageReport d WHERE 
            d.isDeleted = false 
            AND (:startDate IS NULL OR CAST(d.reportedAt AS date) >= :startDate) 
            AND (:endDate IS NULL OR CAST(d.reportedAt AS date) <= :endDate)
            """)
    BigDecimal averageRepairCost(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);


    @Query("""
            SELECT COUNT(d) FROM DamageReport d 
            WHERE d.isDeleted = false 
            AND d.status IN (com.akif.damage.domain.enums.DamageStatus.REPORTED, com.akif.damage.domain.enums.DamageStatus.UNDER_ASSESSMENT)
            """)
    int countPendingAssessments();

    @Query("""
            SELECT COUNT(d) FROM DamageReport d 
            WHERE d.isDeleted = false 
            AND d.status = com.akif.damage.domain.enums.DamageStatus.DISPUTED 
            AND d.reportedAt < :cutoffDate
            """)
    int countUnresolvedDisputesOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}