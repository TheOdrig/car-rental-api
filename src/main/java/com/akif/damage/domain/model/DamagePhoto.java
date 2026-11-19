package com.akif.damage.domain.model;

import com.akif.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "damage_photos",
        indexes = {
                @Index(name = "idx_damage_photos_report", columnList = "damage_report_id")
        })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DamagePhoto extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "damage_report_id", nullable = false, foreignKey = @ForeignKey(name = "fk_damage_photo_report"))
    private DamageReport damageReport;
    
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;
    
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;
    
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    
    @Column(name = "content_type", length = 100)
    private String contentType;
    
    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;
    
    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;
    
    @Column(name = "display_order")
    private Integer displayOrder;
}
