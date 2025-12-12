package com.akif.rental.internal.repository;

import com.akif.rental.domain.enums.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.domain.enums.LateReturnStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RentalRepository extends JpaRepository<Rental, Long> {

    Page<Rental> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

    Page<Rental> findByCarIdAndIsDeletedFalse(Long carId, Pageable pageable);

    Page<Rental> findByStatusAndIsDeletedFalse(RentalStatus status, Pageable pageable);

    Optional<Rental> findByIdAndIsDeletedFalse(Long id);

    Page<Rental> findByIsDeletedFalse(Pageable pageable);

    @Query("SELECT COUNT(r) FROM Rental r " +
            "WHERE r.carId = :carId " +
            "AND r.status IN (com.akif.rental.domain.enums.RentalStatus.CONFIRMED, com.akif.rental.domain.enums.RentalStatus.IN_USE) " +
            "AND r.isDeleted = false " +
            "AND ((r.startDate <= :endDate AND r.endDate >= :startDate))")
    long countOverlappingRentals(@Param("carId") Long carId,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);

    @Query("SELECT r FROM Rental r " +
            "WHERE r.startDate = :tomorrow " +
            "AND r.status = :status " +
            "AND r.pickupReminderSent = false " +
            "AND r.isDeleted = false")
    Page<Rental> findRentalsForPickupReminder(@Param("tomorrow") LocalDate tomorrow,
                                               @Param("status") RentalStatus status,
                                               Pageable pageable);

    @Query("SELECT r FROM Rental r " +
            "WHERE r.endDate = :today " +
            "AND r.status = :status " +
            "AND r.returnReminderSent = false " +
            "AND r.isDeleted = false")
    Page<Rental> findRentalsForReturnReminder(@Param("today") LocalDate today,
                                               @Param("status") RentalStatus status,
                                               Pageable pageable);

    @Query("SELECT r FROM Rental r WHERE " +
            "r.carId = :carId AND " +
            "r.startDate <= :endDate AND " +
            "r.endDate >= :startDate AND " +
            "r.status IN :statuses AND " +
            "r.isDeleted = false")
    List<Rental> findOverlappingRentalsForCar(
            @Param("carId") Long carId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<RentalStatus> statuses);

    @Query("SELECT r FROM Rental r " +
            "WHERE r.status = com.akif.rental.domain.enums.RentalStatus.IN_USE " +
            "AND r.endDate < :currentDate " +
            "AND r.isDeleted = false")
    Page<Rental> findOverdueRentals(@Param("currentDate") LocalDate currentDate,
                                     Pageable pageable);

    @Query("SELECT r FROM Rental r " +
            "WHERE r.lateReturnStatus IN (:statuses) " +
            "AND (:startDate IS NULL OR r.endDate >= :startDate) " +
            "AND (:endDate IS NULL OR r.endDate <= :endDate) " +
            "AND r.isDeleted = false")
    Page<Rental> findLateReturns(@Param("statuses") List<LateReturnStatus> statuses,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate,
                                  Pageable pageable);

    @Query("SELECT COUNT(r) FROM Rental r " +
            "WHERE r.lateReturnStatus IN (:statuses) " +
            "AND (:startDate IS NULL OR r.endDate >= :startDate) " +
            "AND (:endDate IS NULL OR r.endDate <= :endDate) " +
            "AND r.isDeleted = false")
    long countLateReturns(@Param("statuses") List<LateReturnStatus> statuses,
                          @Param("startDate") LocalDate startDate,
                          @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(r) FROM Rental r " +
            "WHERE r.lateReturnStatus = com.akif.rental.domain.enums.LateReturnStatus.SEVERELY_LATE " +
            "AND (:startDate IS NULL OR r.endDate >= :startDate) " +
            "AND (:endDate IS NULL OR r.endDate <= :endDate) " +
            "AND r.isDeleted = false")
    long countSeverelyLateReturns(@Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(r.penaltyAmount), 0) FROM Rental r " +
            "WHERE r.lateReturnStatus IN (:statuses) " +
            "AND (:startDate IS NULL OR r.endDate >= :startDate) " +
            "AND (:endDate IS NULL OR r.endDate <= :endDate) " +
            "AND r.isDeleted = false")
    BigDecimal sumTotalPenaltyAmount(@Param("statuses") List<LateReturnStatus> statuses,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(r.penaltyAmount), 0) FROM Rental r " +
            "WHERE r.lateReturnStatus IN (:statuses) " +
            "AND r.penaltyPaid = true " +
            "AND (:startDate IS NULL OR r.endDate >= :startDate) " +
            "AND (:endDate IS NULL OR r.endDate <= :endDate) " +
            "AND r.isDeleted = false")
    BigDecimal sumCollectedPenaltyAmount(@Param("statuses") List<LateReturnStatus> statuses,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(AVG(r.lateHours), 0.0) FROM Rental r " +
            "WHERE r.lateReturnStatus IN (:statuses) " +
            "AND (:startDate IS NULL OR r.endDate >= :startDate) " +
            "AND (:endDate IS NULL OR r.endDate <= :endDate) " +
            "AND r.lateHours IS NOT NULL " +
            "AND r.isDeleted = false")
    Double averageLateHours(@Param("statuses") List<LateReturnStatus> statuses,
                            @Param("startDate") LocalDate startDate,
                            @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(r) FROM Rental r " +
            "WHERE (:startDate IS NULL OR r.endDate >= :startDate) " +
            "AND (:endDate IS NULL OR r.endDate <= :endDate) " +
            "AND r.status = com.akif.rental.domain.enums.RentalStatus.RETURNED " +
            "AND r.isDeleted = false")
    long countTotalReturns(@Param("startDate") LocalDate startDate,
                           @Param("endDate") LocalDate endDate);
}