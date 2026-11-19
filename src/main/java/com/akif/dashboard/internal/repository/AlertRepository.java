package com.akif.dashboard.internal.repository;

import com.akif.dashboard.api.AlertType;
import com.akif.dashboard.domain.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    @Query("""
            SELECT a FROM Alert a 
            WHERE a.acknowledged = false 
            AND a.isDeleted = false 
            ORDER BY a.severity ASC
            """)
    List<Alert> findByAcknowledgedFalseOrderBySeverityAsc();

    @Query("""
            SELECT a FROM Alert a 
            WHERE a.type = :type 
            AND a.acknowledged = false 
            AND a.isDeleted = false 
            ORDER BY a.severity ASC
            """)
    List<Alert> findByTypeAndAcknowledgedFalse(@Param("type") AlertType type);

    @Query("""
            SELECT COUNT(a) > 0 FROM Alert a 
            WHERE a.type = :type 
            AND a.referenceId = :referenceId 
            AND a.acknowledged = false 
            AND a.isDeleted = false
            """)
    boolean existsByTypeAndReferenceIdAndAcknowledgedFalse(
            @Param("type") AlertType type,
            @Param("referenceId") Long referenceId);

    @Query("""
            SELECT COUNT(a) > 0 FROM Alert a
            WHERE a.type = :type
            AND a.referenceId = :referenceId
            AND a.title = :title
            AND a.message LIKE CONCAT('%', :marker, '%')
            AND a.acknowledged = false
            AND a.isDeleted = false
            """)
    boolean existsOpenAlertForPayment(
            @Param("type") AlertType type,
            @Param("referenceId") Long referenceId,
            @Param("title") String title,
            @Param("marker") String marker);

    @Query("""
            SELECT COUNT(a) > 0 FROM Alert a
            WHERE a.type = :type
            AND a.referenceId = :referenceId
            AND a.title = :title
            AND a.acknowledged = false
            AND a.isDeleted = false
            """)
    boolean existsByTypeAndReferenceIdAndTitleAndAcknowledgedFalse(
            @Param("type") AlertType type,
            @Param("referenceId") Long referenceId,
            @Param("title") String title);
}
