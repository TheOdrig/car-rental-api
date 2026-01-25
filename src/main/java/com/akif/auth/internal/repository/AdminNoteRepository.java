package com.akif.auth.internal.repository;

import com.akif.auth.domain.AdminNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminNoteRepository extends JpaRepository<AdminNote, Long> {

    List<AdminNote> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId);
}
