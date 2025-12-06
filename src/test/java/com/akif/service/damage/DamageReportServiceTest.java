package com.akif.service.damage;

import com.akif.config.DamageConfig;
import com.akif.dto.damage.request.DamageReportRequestDto;
import com.akif.dto.damage.response.DamagePhotoDto;
import com.akif.dto.damage.response.DamageReportResponseDto;
import com.akif.enums.DamageCategory;
import com.akif.enums.DamageSeverity;
import com.akif.enums.DamageStatus;
import com.akif.event.DamageReportedEvent;
import com.akif.exception.DamageReportException;
import com.akif.exception.FileUploadException;
import com.akif.exception.RentalNotFoundException;
import com.akif.model.*;
import com.akif.repository.DamagePhotoRepository;
import com.akif.repository.DamageReportRepository;
import com.akif.repository.RentalRepository;
import com.akif.repository.UserRepository;
import com.akif.service.damage.impl.DamageReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DamageReportService Unit Tests")
class DamageReportServiceTest {

    @Mock
    private DamageReportRepository damageReportRepository;

    @Mock
    private DamagePhotoRepository damagePhotoRepository;

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IFileUploadService fileUploadService;

    @Mock
    private DamageConfig damageConfig;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DamageReportServiceImpl damageReportService;

    private Rental testRental;
    private Car testCar;
    private User testUser;
    private DamageReport testDamageReport;
    private DamagePhoto testDamagePhoto;
    private DamageReportRequestDto testRequestDto;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        testCar = Car.builder()
                .id(1L)
                .licensePlate("34ABC123")
                .brand("Toyota")
                .model("Corolla")
                .build();

        testRental = Rental.builder()
                .id(1L)
                .user(testUser)
                .car(testCar)
                .hasDamageReports(false)
                .damageReportsCount(0)
                .build();

        testDamageReport = DamageReport.builder()
                .id(1L)
                .rental(testRental)
                .car(testCar)
                .description("Scratch on front bumper")
                .damageLocation("Front bumper")
                .severity(DamageSeverity.MINOR)
                .category(DamageCategory.SCRATCH)
                .status(DamageStatus.REPORTED)
                .reportedBy(1L)
                .reportedAt(LocalDateTime.now())
                .build();

        testDamagePhoto = DamagePhoto.builder()
                .id(1L)
                .damageReport(testDamageReport)
                .fileName("damage-photo.jpg")
                .filePath("uploads/damage-photos/damage-photo.jpg")
                .fileSize(1024L)
                .contentType("image/jpeg")
                .uploadedBy(1L)
                .uploadedAt(LocalDateTime.now())
                .displayOrder(1)
                .build();

        testRequestDto = new DamageReportRequestDto(
                "Scratch on front bumper",
                "Front bumper",
                DamageSeverity.MINOR,
                DamageCategory.SCRATCH
        );
    }

    @Nested
    @DisplayName("Create Damage Report Operations")
    class CreateDamageReportOperations {

        @Test
        @DisplayName("Should create damage report successfully")
        void shouldCreateDamageReportSuccessfully() {
            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(userRepository.findByUsernameAndIsDeletedFalse("testuser")).thenReturn(Optional.of(testUser));
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);
            when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);
            when(damagePhotoRepository.findByDamageReportIdAndIsDeletedFalse(any())).thenReturn(List.of());
            when(fileUploadService.generateSecureUrl(anyString(), anyInt())).thenReturn("https://example.com/photo.jpg");

            DamageReportResponseDto result = damageReportService.createDamageReport(1L, testRequestDto, "testuser");

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.description()).isEqualTo("Scratch on front bumper");
            assertThat(result.status()).isEqualTo(DamageStatus.REPORTED);

            verify(damageReportRepository).save(any(DamageReport.class));
            verify(rentalRepository).save(testRental);
            verify(eventPublisher).publishEvent(any(DamageReportedEvent.class));
        }

        @Test
        @DisplayName("Should update rental damage count when creating report")
        void shouldUpdateRentalDamageCountWhenCreatingReport() {
            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(userRepository.findByUsernameAndIsDeletedFalse("testuser")).thenReturn(Optional.of(testUser));
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);
            when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);
            when(damagePhotoRepository.findByDamageReportIdAndIsDeletedFalse(any())).thenReturn(List.of());

            damageReportService.createDamageReport(1L, testRequestDto, "testuser");

            ArgumentCaptor<Rental> rentalCaptor = ArgumentCaptor.forClass(Rental.class);
            verify(rentalRepository).save(rentalCaptor.capture());

            Rental savedRental = rentalCaptor.getValue();
            assertThat(savedRental.getHasDamageReports()).isTrue();
            assertThat(savedRental.getDamageReportsCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should throw exception when rental not found")
        void shouldThrowExceptionWhenRentalNotFound() {
            when(rentalRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> damageReportService.createDamageReport(999L, testRequestDto, "testuser"))
                    .isInstanceOf(RentalNotFoundException.class)
                    .hasMessageContaining("Rental not found");

            verify(damageReportRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(userRepository.findByUsernameAndIsDeletedFalse("unknownuser")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> damageReportService.createDamageReport(1L, testRequestDto, "unknownuser"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");

            verify(damageReportRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should publish DamageReportedEvent when creating report")
        void shouldPublishDamageReportedEventWhenCreatingReport() {
            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(userRepository.findByUsernameAndIsDeletedFalse("testuser")).thenReturn(Optional.of(testUser));
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);
            when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);
            when(damagePhotoRepository.findByDamageReportIdAndIsDeletedFalse(any())).thenReturn(List.of());

            damageReportService.createDamageReport(1L, testRequestDto, "testuser");

            ArgumentCaptor<DamageReportedEvent> eventCaptor = ArgumentCaptor.forClass(DamageReportedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            DamageReportedEvent event = eventCaptor.getValue();
            assertThat(event.getDamageReport()).isEqualTo(testDamageReport);
        }
    }

    @Nested
    @DisplayName("Get Damage Report Operations")
    class GetDamageReportOperations {

        @Test
        @DisplayName("Should get damage report successfully")
        void shouldGetDamageReportSuccessfully() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(damagePhotoRepository.findByDamageReportIdAndIsDeletedFalse(1L)).thenReturn(List.of());

            DamageReportResponseDto result = damageReportService.getDamageReport(1L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.description()).isEqualTo("Scratch on front bumper");
        }

        @Test
        @DisplayName("Should throw exception when damage report not found")
        void shouldThrowExceptionWhenDamageReportNotFound() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> damageReportService.getDamageReport(999L))
                    .isInstanceOf(DamageReportException.class);
        }
    }

    @Nested
    @DisplayName("Upload Damage Photos Operations")
    class UploadDamagePhotosOperations {

        @Test
        @DisplayName("Should upload photos successfully")
        void shouldUploadPhotosSuccessfully() {
            MultipartFile mockFile = new MockMultipartFile(
                    "photo",
                    "test-photo.jpg",
                    "image/jpeg",
                    "test content".getBytes()
            );

            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(userRepository.findByUsernameAndIsDeletedFalse("testuser")).thenReturn(Optional.of(testUser));
            when(damagePhotoRepository.countByDamageReportIdAndIsDeletedFalse(1L)).thenReturn(0);
            when(damageConfig.getMaxPhotosPerReport()).thenReturn(10);
            when(damageConfig.getPhotoStorageDirectory()).thenReturn("damage-photos");
            when(damageConfig.getAllowedPhotoTypes()).thenReturn(List.of("image/jpeg", "image/png"));
            when(damageConfig.getMaxPhotoSizeBytes()).thenReturn(5 * 1024 * 1024L);
            when(damageConfig.getPhotoUrlExpirationMinutes()).thenReturn(60);
            when(fileUploadService.validateFileType(any(), anyList())).thenReturn(true);
            when(fileUploadService.validateFileSize(any(), anyLong())).thenReturn(true);
            when(fileUploadService.uploadFile(any(), anyString())).thenReturn("uploads/damage-photos/uuid.jpg");
            when(fileUploadService.generateSecureUrl(anyString(), anyInt())).thenReturn("https://example.com/photo.jpg");
            when(damagePhotoRepository.save(any(DamagePhoto.class))).thenReturn(testDamagePhoto);

            List<DamagePhotoDto> result = damageReportService.uploadDamagePhotos(1L, List.of(mockFile), "testuser");

            assertThat(result).hasSize(1);
            verify(fileUploadService).uploadFile(any(), eq("damage-photos"));
            verify(damagePhotoRepository).save(any(DamagePhoto.class));
        }

        @Test
        @DisplayName("Should throw exception when max photos exceeded")
        void shouldThrowExceptionWhenMaxPhotosExceeded() {
            MultipartFile mockFile = new MockMultipartFile("photo", "test.jpg", "image/jpeg", "test".getBytes());

            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(userRepository.findByUsernameAndIsDeletedFalse("testuser")).thenReturn(Optional.of(testUser));
            when(damagePhotoRepository.countByDamageReportIdAndIsDeletedFalse(1L)).thenReturn(9);
            when(damageConfig.getMaxPhotosPerReport()).thenReturn(10);

            assertThatThrownBy(() -> damageReportService.uploadDamagePhotos(1L, List.of(mockFile, mockFile), "testuser"))
                    .isInstanceOf(DamageReportException.class)
                    .hasMessageContaining("Maximum");

            verify(fileUploadService, never()).uploadFile(any(), anyString());
        }

        @Test
        @DisplayName("Should throw exception for invalid file type")
        void shouldThrowExceptionForInvalidFileType() {
            MultipartFile mockFile = new MockMultipartFile("photo", "test.exe", "application/x-msdownload", "test".getBytes());

            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(userRepository.findByUsernameAndIsDeletedFalse("testuser")).thenReturn(Optional.of(testUser));
            when(damagePhotoRepository.countByDamageReportIdAndIsDeletedFalse(1L)).thenReturn(0);
            when(damageConfig.getMaxPhotosPerReport()).thenReturn(10);
            when(damageConfig.getAllowedPhotoTypes()).thenReturn(List.of("image/jpeg", "image/png"));
            when(fileUploadService.validateFileType(any(), anyList())).thenReturn(false);

            assertThatThrownBy(() -> damageReportService.uploadDamagePhotos(1L, List.of(mockFile), "testuser"))
                    .isInstanceOf(FileUploadException.class)
                    .hasMessageContaining("Invalid file type");
        }

        @Test
        @DisplayName("Should throw exception for oversized file")
        void shouldThrowExceptionForOversizedFile() {
            MultipartFile mockFile = new MockMultipartFile("photo", "large.jpg", "image/jpeg", new byte[10 * 1024 * 1024]);

            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(userRepository.findByUsernameAndIsDeletedFalse("testuser")).thenReturn(Optional.of(testUser));
            when(damagePhotoRepository.countByDamageReportIdAndIsDeletedFalse(1L)).thenReturn(0);
            when(damageConfig.getMaxPhotosPerReport()).thenReturn(10);
            when(damageConfig.getAllowedPhotoTypes()).thenReturn(List.of("image/jpeg"));
            when(damageConfig.getMaxPhotoSizeBytes()).thenReturn(5 * 1024 * 1024L);
            when(fileUploadService.validateFileType(any(), anyList())).thenReturn(true);
            when(fileUploadService.validateFileSize(any(), anyLong())).thenReturn(false);

            assertThatThrownBy(() -> damageReportService.uploadDamagePhotos(1L, List.of(mockFile), "testuser"))
                    .isInstanceOf(FileUploadException.class)
                    .hasMessageContaining("File size exceeds");
        }
    }

    @Nested
    @DisplayName("Delete Damage Photo Operations")
    class DeleteDamagePhotoOperations {

        @Test
        @DisplayName("Should delete photo successfully")
        void shouldDeletePhotoSuccessfully() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(damagePhotoRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamagePhoto));

            damageReportService.deleteDamagePhoto(1L, 1L);

            verify(fileUploadService).deleteFile(testDamagePhoto.getFilePath());
            verify(damagePhotoRepository).save(testDamagePhoto);
            assertThat(testDamagePhoto.getIsDeleted()).isTrue();
        }

        @Test
        @DisplayName("Should throw exception when photo not found")
        void shouldThrowExceptionWhenPhotoNotFound() {
            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(damagePhotoRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> damageReportService.deleteDamagePhoto(1L, 999L))
                    .isInstanceOf(DamageReportException.class);
        }

        @Test
        @DisplayName("Should throw exception when photo belongs to different report")
        void shouldThrowExceptionWhenPhotoBelongsToDifferentReport() {
            DamageReport otherReport = DamageReport.builder().id(2L).build();
            DamagePhoto photoFromOtherReport = DamagePhoto.builder()
                    .id(2L)
                    .damageReport(otherReport)
                    .filePath("some/path.jpg")
                    .build();

            when(damageReportRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamageReport));
            when(damagePhotoRepository.findByIdAndIsDeletedFalse(2L)).thenReturn(Optional.of(photoFromOtherReport));

            assertThatThrownBy(() -> damageReportService.deleteDamagePhoto(1L, 2L))
                    .isInstanceOf(DamageReportException.class)
                    .hasMessageContaining("does not belong");
        }
    }

    @Nested
    @DisplayName("Get Photo URL Operations")
    class GetPhotoUrlOperations {

        @Test
        @DisplayName("Should get photo URL successfully")
        void shouldGetPhotoUrlSuccessfully() {
            when(damagePhotoRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testDamagePhoto));
            when(damageConfig.getPhotoUrlExpirationMinutes()).thenReturn(60);
            when(fileUploadService.generateSecureUrl(anyString(), eq(60))).thenReturn("https://secure.url/photo.jpg");

            String result = damageReportService.getPhotoUrl(1L);

            assertThat(result).isEqualTo("https://secure.url/photo.jpg");
            verify(fileUploadService).generateSecureUrl(testDamagePhoto.getFilePath(), 60);
        }

        @Test
        @DisplayName("Should throw exception when photo not found for URL")
        void shouldThrowExceptionWhenPhotoNotFoundForUrl() {
            when(damagePhotoRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> damageReportService.getPhotoUrl(999L))
                    .isInstanceOf(DamageReportException.class);
        }
    }
}
