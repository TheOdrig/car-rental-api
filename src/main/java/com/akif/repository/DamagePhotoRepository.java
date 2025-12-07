package com.akif.repository;

import com.akif.model.DamagePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DamagePhotoRepository extends JpaRepository<DamagePhoto, Long> {

    Optional<DamagePhoto> findByIdAndIsDeletedFalse(Long id);

    List<DamagePhoto> findByDamageReportIdAndIsDeletedFalse(Long damageReportId);

    int countByDamageReportIdAndIsDeletedFalse(Long damageReportId);
}
