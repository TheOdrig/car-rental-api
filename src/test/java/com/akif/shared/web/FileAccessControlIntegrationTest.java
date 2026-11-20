package com.akif.shared.web;

import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.damage.api.DamageCategory;
import com.akif.damage.api.DamageSeverity;
import com.akif.damage.domain.model.DamagePhoto;
import com.akif.damage.domain.model.DamageReport;
import com.akif.damage.internal.repository.DamagePhotoRepository;
import com.akif.damage.internal.repository.DamageReportRepository;
import com.akif.shared.enums.Role;
import com.akif.shared.security.JwtTokenService;
import com.akif.testsupport.ImageFixtures;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Local file access control")
class FileAccessControlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DamageReportRepository damageReportRepository;

    @Autowired
    private DamagePhotoRepository damagePhotoRepository;

    @Autowired
    private JwtTokenService tokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${file.upload.base-dir}")
    private String baseDir;

    private User owner;
    private User other;
    private User admin;
    private String photoName;
    private String avatarName;

    @BeforeEach
    void setUp() throws IOException {
        jdbcTemplate.execute("TRUNCATE TABLE car_rental.damage_photos, car_rental.damage_reports, "
                + "car_rental.users RESTART IDENTITY CASCADE");
        owner = user("fileowner", Role.USER);
        other = user("fileother", Role.USER);
        admin = user("fileadmin", Role.ADMIN);

        photoName = UUID.randomUUID() + ".jpg";
        avatarName = UUID.randomUUID() + ".jpg";
        write("damage-photos", photoName);
        write("avatars", avatarName);

        DamageReport report = damageReportRepository.save(DamageReport.builder()
                .rentalId(9001L)
                .carId(9002L)
                .carBrand("Toyota")
                .carModel("Corolla")
                .carLicensePlate("34FIL001")
                .rentalStartDate(LocalDate.now().minusDays(3))
                .rentalEndDate(LocalDate.now().minusDays(1))
                .customerEmail(owner.getEmail())
                .customerFullName("File Owner")
                .customerUserId(owner.getId())
                .description("Scratch on the door")
                .severity(DamageSeverity.MINOR)
                .category(DamageCategory.SCRATCH)
                .reportedBy(admin.getId())
                .reportedAt(Instant.now())
                .build());
        damagePhotoRepository.save(DamagePhoto.builder()
                .damageReport(report)
                .fileName("door.jpg")
                .filePath(storedPath("damage-photos", photoName))
                .fileSize(1L)
                .contentType("image/jpeg")
                .uploadedBy(admin.getId())
                .uploadedAt(Instant.now())
                .displayOrder(0)
                .build());
        owner.changeAvatar(storedPath("avatars", avatarName));
        userRepository.save(owner);
    }

    private String storedPath(String directory, String name) {
        return Paths.get(baseDir).resolve(directory).resolve(name).toString().replace("\\", "/");
    }

    private User user(String username, Role role) {
        return userRepository.save(User.builder()
                .username(username)
                .email(username + "@example.com")
                .password("x")
                .firstName("File")
                .lastName(username)
                .roles(Set.of(role))
                .enabled(true)
                .build());
    }

    private void write(String directory, String name) throws IOException {
        Path dir = Paths.get(baseDir).toAbsolutePath().resolve(directory);
        Files.createDirectories(dir);
        Files.write(dir.resolve(name), ImageFixtures.jpeg());
    }

    private MockHttpServletRequestBuilder as(User caller, String url) {
        String role = caller.getRoles().contains(Role.ADMIN) ? "ROLE_ADMIN" : "ROLE_USER";
        String token = tokenService.generateAccessToken(new UsernamePasswordAuthenticationToken(
                caller.getUsername(), null, List.of(new SimpleGrantedAuthority(role))), caller.getId());
        return get(url).header("Authorization", "Bearer " + token);
    }

    @Nested
    @DisplayName("damage photos")
    class DamagePhotos {

        private String url() {
            return "/api/v1/files/damage-photos/" + photoName;
        }

        @Test
        @DisplayName("the rental owner receives the photo with a safe content type")
        void ownerReads() throws Exception {
            mockMvc.perform(as(owner, url()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("image/jpeg"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        }

        @Test
        @DisplayName("an admin receives the photo")
        void adminReads() throws Exception {
            mockMvc.perform(as(admin, url())).andExpect(status().isOk());
        }

        @Test
        @DisplayName("another customer gets 404")
        void otherCannotRead() throws Exception {
            mockMvc.perform(as(other, url())).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("an anonymous caller gets 401")
        void anonymousIsRejected() throws Exception {
            mockMvc.perform(get(url())).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("an unknown file is 404 for a customer")
        void unknownFile() throws Exception {
            mockMvc.perform(as(owner, "/api/v1/files/damage-photos/" + UUID.randomUUID() + ".jpg"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("an admin asking for a missing file gets 404 problem+json")
        void missingFileForAdminIsProblemJson() throws Exception {
            mockMvc.perform(as(admin, "/api/v1/files/damage-photos/" + UUID.randomUUID() + ".jpg")
                            .accept("image/avif,image/webp,image/png,image/*;q=0.8,*/*;q=0.5"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        }

        @Test
        @DisplayName("the photo response states its Content-Length")
        void photoStatesContentLength() throws Exception {
            mockMvc.perform(as(owner, url()))
                    .andExpect(status().isOk())
                    .andExpect(header().longValue("Content-Length", ImageFixtures.jpeg().length));
        }
    }

    @Nested
    @DisplayName("avatars")
    class Avatars {

        private String url() {
            return "/api/v1/files/avatars/" + avatarName;
        }

        @Test
        @DisplayName("the avatar owner receives the image with a safe content type")
        void ownerReads() throws Exception {
            mockMvc.perform(as(owner, url()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("image/jpeg"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        }

        @Test
        @DisplayName("an admin receives the image")
        void adminReads() throws Exception {
            mockMvc.perform(as(admin, url())).andExpect(status().isOk());
        }

        @Test
        @DisplayName("another customer gets 404")
        void otherCannotRead() throws Exception {
            mockMvc.perform(as(other, url())).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("an anonymous caller gets 401")
        void anonymousIsRejected() throws Exception {
            mockMvc.perform(get(url())).andExpect(status().isUnauthorized());
        }
    }
}
