package com.akif.repository;

import com.akif.enums.DamageCategory;
import com.akif.enums.DamageSeverity;
import com.akif.enums.DamageStatus;
import com.akif.model.DamageReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DamageReportRepository extends JpaRepository<DamageReport, Long> {

    Optional<DamageReport> findByIdAndIsDeletedFalse(Long id);

    Page<DamageReport> findByRentalIdAndIsDeletedFalse(Long rentalId, Pageable pageable);

    Page<DamageReport> findByCarIdAndIsDeletedFalse(Long carId, Pageable pageable);

    Page<DamageReport> findByRental_UserIdAndIsDeletedFalse(Long userId, Pageable pageable);

    Page<DamageReport> findByStatusAndIsDeletedFalse(DamageStatus status, Pageable pageable);

    Optional<DamageReport> findByPaymentId(Long paymentId);

    @Query("SELECT d FROM DamageReport d WHERE " +
            "d.isDeleted = false " +
            "AND (:startDate IS NULL OR CAST(d.reportedAt AS date) >= :startDate) " +
            "AND (:endDate IS NULL OR CAST(d.reportedAt AS date) <= :endDate) " +
            "AND (:severity IS NULL OR d.severity = :severity) " +
            "AND (:category IS NULL OR d.category = :category) " +
            "AND (:status IS NULL OR d.status = :status) " +
            "AND (:carId IS NULL OR d.car.id = :carId) " +
            "AND (:customerId IS NULL OR d.rental.user.id = :customerId)")
    Page<DamageReport> searchDamages(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("severity") DamageSeverity severity,
            @Param("category") DamageCategory category,
            @Param("status") DamageStatus status,
            @Param("carId") Long carId,
            @Param("customerId") Long customerId,
            Pageable pageable);

    @Query("SELECT COUNT(d) FROM DamageReport d WHERE " +
            "d.isDeleted = false " +
            "AND (:startDate IS NULL OR CAST(d.reportedAt AS date) >= :startDate) " +
            "AND (:endDate IS NULL OR CAST(d.reportedAt AS date) <= :endDate)")
    int countTotalDamages(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(d) FROM DamageReport d WHERE " +
            "d.isDeleted = false " +
            "AND d.severity = :severity " +
            "AND (:startDate IS NULL OR CAST(d.reportedAt AS date) >= :startDate) " +
            "AND (:endDate IS NULL OR CAST(d.reportedAt AS date) <= :endDate)")
    int countBySeverity(
            @Param("severity") DamageSeverity severity,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(d) FROM DamageReport d WHERE " +
            "d.isDeleted = false " +
            "AND d.status = :status " +
            "AND (:startDate IS NULL OR CAST(d.reportedAt AS date) >= :startDate) " +
            "AND (:endDate IS NULL OR CAST(d.reportedAt AS date) <= :endDate)")
    int countByStatus(
            @Param("status") DamageStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(d.repairCostEstimate), 0) FROM DamageReport d WHERE " +
            "d.isDeleted = false " +
            "AND (:startDate IS NULL OR CAST(d.reportedAt AS date) >= :startDate) " +
            "AND (:endDate IS NULL OR CAST(d.reportedAt AS date) <= :endDate)")
    BigDecimal sumTotalRepairCost(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(d.customerLiability), 0) FROM DamageReport d WHERE " +
            "d.isDeleted = false " +
            "AND (:startDate IS NULL OR CAST(d.reportedAt AS date) >= :startDate) " +
            "AND (:endDate IS NULL OR CAST(d.reportedAt AS date) <= :endDate)")
    BigDecimal sumTotalCustomerLiability(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(AVG(d.repairCostEstimate), 0) FROM DamageReport d WHERE " +
            "d.isDeleted = false " +
            "AND (:startDate IS NULL OR CAST(d.reportedAt AS date) >= :startDate) " +
            "AND (:endDate IS NULL OR CAST(d.reportedAt AS date) <= :endDate)")
    BigDecimal averageRepairCost(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
