package com.akif.repository;

import com.akif.enums.RentalStatus;
import com.akif.model.Rental;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface RentalRepository extends JpaRepository<Rental, Long> {

    Page<Rental> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

    Page<Rental> findByCarIdAndIsDeletedFalse(Long carId, Pageable pageable);

    Page<Rental> findByStatusAndIsDeletedFalse(RentalStatus status, Pageable pageable);

    Optional<Rental> findByIdAndIsDeletedFalse(Long id);

    Page<Rental> findByIsDeletedFalse(Pageable pageable);

    @Query("SELECT COUNT(r) FROM Rental r " +
            "WHERE r.car.id = :carId " +
            "AND r.status IN (com.akif.enums.RentalStatus.CONFIRMED, com.akif.enums.RentalStatus.IN_USE) " +
            "AND r.isDeleted = false " +
            "AND ((r.startDate <= :endDate AND r.endDate >= :startDate))")
    long countOverlappingRentals(@Param("carId") Long carId,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);
}