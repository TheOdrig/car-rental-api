package com.akif.damage.internal.repository;

import com.akif.damage.domain.model.DamagePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DamagePhotoRepository extends JpaRepository<DamagePhoto, Long> {

    Optional<DamagePhoto> findByIdAndIsDeletedFalse(Long id);

    List<DamagePhoto> findByDamageReportIdAndIsDeletedFalse(Long damageReportId);

    int countByDamageReportIdAndIsDeletedFalse(Long damageReportId);

    @Query(value = """
            SELECT d.customer_user_id FROM car_rental.damage_photos p
            JOIN car_rental.damage_reports d ON d.id = p.damage_report_id
            WHERE substring(p.file_path from '[^/]+$') = :filename
            AND p.is_deleted = false AND d.is_deleted = false
            """, nativeQuery = true)
    List<Long> findCustomerIdsByFileName(@Param("filename") String filename);
}
