package com.akif.damage.unit;

import com.akif.auth.api.AuthService;
import com.akif.auth.api.UserDto;
import com.akif.car.api.CarDto;
import com.akif.car.api.CarService;
import com.akif.car.domain.enums.CarStatusType;
import com.akif.damage.api.DamageReportedEvent;
import com.akif.damage.domain.enums.DamageCategory;
import com.akif.damage.domain.enums.DamageSeverity;
import com.akif.damage.domain.enums.DamageStatus;
import com.akif.damage.domain.model.DamagePhoto;
import com.akif.damage.domain.model.DamageReport;
import com.akif.damage.internal.config.DamageConfig;
import com.akif.damage.internal.dto.damage.request.DamageReportRequest;
import com.akif.damage.internal.dto.damage.response.DamagePhotoDto;
import com.akif.damage.internal.dto.damage.response.DamageReportResponse;
import com.akif.damage.internal.exception.DamageReportException;
import com.akif.damage.internal.repository.DamagePhotoRepository;
import com.akif.damage.internal.repository.DamageReportRepository;
import com.akif.damage.internal.service.damage.impl.DamageReportServiceImpl;
import com.akif.rental.api.RentalService;
import com.akif.rental.api.RentalSummaryDto;
import com.akif.rental.internal.exception.RentalNotFoundException;
import com.akif.shared.enums.CurrencyType;
import com.akif.shared.exception.FileUploadException;
import com.akif.shared.infrastructure.FileUploadService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.akif.shared.enums.Role;

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
    private RentalService rentalService;

    @Mock
    private CarService carService;

    @Mock
    private AuthService authService;

    @Mock
    private FileUploadService fileUploadService;

    @Mock
    private DamageConfig damageConfig;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DamageReportServiceImpl damageReportService;

    private RentalSummaryDto testRental;
    private CarDto testCar;
    private UserDto testUser;
    private DamageReport testDamageReport;
    private DamagePhoto testDamagePhoto;
    private DamageReportRequest testRequestDto;

    @BeforeEach
    void setUp() {
        testUser = new UserDto(
                1L,
                "testuser",
                "test@example.com",
                "Test",
                "User",
                Set.of(Role.USER),
                true
        );

        testCar = new CarDto(
                1L,
                "Toyota",
                "Corolla",
                "34ABC123",
                new BigDecimal("500.00"),
                CurrencyType.TRY,
                CarStatusType.AVAILABLE,
                "SEDAN",
                5,
                true,
                false
        );

        testRental = new RentalSummaryDto(
                1L,
                testCar.id(),
                testUser.id(),
                testCar.brand(),
                testCar.model(),
                testCar.licensePlate(),
                testUser.email(),
                testUser.firstName() + " " + testUser.lastName(),
                LocalDate.now().minusDays(5),
                LocalDate.now().plusDays(2),
                false,
                0
        );

        testDamageReport = DamageReport.builder()
                .id(1L)
                .rentalId(testRental.id())
                .carId(testCar.id())
                .carBrand(testCar.brand())
                .carModel(testCar.model())
                .carLicensePlate(testCar.licensePlate())
                .customerEmail(testUser.email())
                .customerFullName(testUser.firstName() + " " + testUser.lastName())
                .customerUserId(testUser.id())
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

        testRequestDto = new DamageReportRequest(
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
            when(rentalService.getRentalSummaryById(1L)).thenReturn(testRental);
            when(authService.getUserByUsername("testuser")).thenReturn(testUser);
            when(carService.getCarDtoById(testCar.id())).thenReturn(testCar);
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);
            when(damagePhotoRepository.findByDamageReportIdAndIsDeletedFalse(any())).thenReturn(List.of());
            when(fileUploadService.generateSecureUrl(anyString(), anyInt())).thenReturn("https://example.com/photo.jpg");

            DamageReportResponse result = damageReportService.createDamageReport(1L, testRequestDto, "testuser");

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.description()).isEqualTo("Scratch on front bumper");
            assertThat(result.status()).isEqualTo(DamageStatus.REPORTED);

            verify(damageReportRepository).save(any(DamageReport.class));
            verify(rentalService).incrementDamageReportCount(1L);
            verify(eventPublisher).publishEvent(any(DamageReportedEvent.class));
        }

        @Test
        @DisplayName("Should call incrementDamageReportCount when creating report")
        void shouldCallIncrementDamageReportCountWhenCreatingReport() {
            when(rentalService.getRentalSummaryById(1L)).thenReturn(testRental);
            when(authService.getUserByUsername("testuser")).thenReturn(testUser);
            when(carService.getCarDtoById(testCar.id())).thenReturn(testCar);
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);
            when(damagePhotoRepository.findByDamageReportIdAndIsDeletedFalse(any())).thenReturn(List.of());

            damageReportService.createDamageReport(1L, testRequestDto, "testuser");

            verify(rentalService).incrementDamageReportCount(1L);
        }

        @Test
        @DisplayName("Should throw exception when rental not found")
        void shouldThrowExceptionWhenRentalNotFound() {
            when(rentalService.getRentalSummaryById(999L))
                    .thenThrow(new RentalNotFoundException("Rental not found with id: 999"));

            assertThatThrownBy(() -> damageReportService.createDamageReport(999L, testRequestDto, "testuser"))
                    .isInstanceOf(RentalNotFoundException.class)
                    .hasMessageContaining("Rental not found");

            verify(damageReportRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(rentalService.getRentalSummaryById(1L)).thenReturn(testRental);
            when(authService.getUserByUsername("unknownuser"))
                    .thenThrow(new RuntimeException("User not found: unknownuser"));

            assertThatThrownBy(() -> damageReportService.createDamageReport(1L, testRequestDto, "unknownuser"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");

            verify(damageReportRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should publish DamageReportedEvent when creating report")
        void shouldPublishDamageReportedEventWhenCreatingReport() {
            when(rentalService.getRentalSummaryById(1L)).thenReturn(testRental);
            when(authService.getUserByUsername("testuser")).thenReturn(testUser);
            when(carService.getCarDtoById(testCar.id())).thenReturn(testCar);
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(testDamageReport);
            when(damagePhotoRepository.findByDamageReportIdAndIsDeletedFalse(any())).thenReturn(List.of());

            damageReportService.createDamageReport(1L, testRequestDto, "testuser");

            ArgumentCaptor<DamageReportedEvent> eventCaptor = ArgumentCaptor.forClass(DamageReportedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            DamageReportedEvent event = eventCaptor.getValue();
            assertThat(event.getDamageReportId()).isEqualTo(testDamageReport.getId());
        }

        @Test
        @DisplayName("Should mark car as maintenance for MAJOR damage")
        void shouldMarkCarAsMaintenanceForMajorDamage() {
            DamageReportRequest majorDamageRequest = new DamageReportRequest(
                    "Engine damage",
                    "Engine",
                    DamageSeverity.MAJOR,
                    DamageCategory.MECHANICAL_DAMAGE
            );

            DamageReport majorDamageReport = DamageReport.builder()
                    .id(2L)
                    .rentalId(testRental.id())
                    .carId(testCar.id())
                    .severity(DamageSeverity.MAJOR)
                    .status(DamageStatus.REPORTED)
                    .build();

            when(rentalService.getRentalSummaryById(1L)).thenReturn(testRental);
            when(authService.getUserByUsername("testuser")).thenReturn(testUser);
            when(carService.getCarDtoById(testCar.id())).thenReturn(testCar);
            when(damageReportRepository.save(any(DamageReport.class))).thenReturn(majorDamageReport);
            when(damagePhotoRepository.findByDamageReportIdAndIsDeletedFalse(any())).thenReturn(List.of());

            damageReportService.createDamageReport(1L, majorDamageRequest, "testuser");

            verify(carService).markAsMaintenance(testCar.id());
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

            DamageReportResponse result = damageReportService.getDamageReport(1L);

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
            when(authService.getUserByUsername("testuser")).thenReturn(testUser);
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
            when(authService.getUserByUsername("testuser")).thenReturn(testUser);
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
            when(authService.getUserByUsername("testuser")).thenReturn(testUser);
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
            when(authService.getUserByUsername("testuser")).thenReturn(testUser);
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
