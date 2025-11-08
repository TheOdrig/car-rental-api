package com.akif.repository;

import com.akif.enums.Role;
import com.akif.model.User;
import com.akif.starter.CarGalleryProjectApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = CarGalleryProjectApplication.class)
@ActiveProfiles("test")
@DisplayName("UserRepository Integration Tests")
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser1;
    private User testUser2;
    private User adminUser;
    private User disabledUser;

    @BeforeEach
    void setUp() {
        testUser1 = User.builder()
                .username("testuser1")
                .email("test1@example.com")
                .password("encodedPassword1")
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();

        testUser2 = User.builder()
                .username("testuser2")
                .email("test2@example.com")
                .password("encodedPassword2")
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();

        adminUser = User.builder()
                .username("adminuser")
                .email("admin@example.com")
                .password("encodedPassword")
                .roles(Set.of(Role.USER, Role.ADMIN))
                .enabled(true)
                .build();

        disabledUser = User.builder()
                .username("disableduser")
                .email("disabled@example.com")
                .password("encodedPassword")
                .roles(Set.of(Role.USER))
                .enabled(false)
                .build();

        userRepository.saveAll(Set.of(testUser1, testUser2, adminUser, disabledUser));
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("Find By Username Tests")
    class FindByUsernameTests {

        @Test
        @DisplayName("Should find user by username")
        void shouldFindUserByUsername() {
            Optional<User> found = userRepository.findByUsername("testuser1");

            assertThat(found).isPresent();
            assertThat(found.get().getUsername()).isEqualTo("testuser1");
            assertThat(found.get().getEmail()).isEqualTo("test1@example.com");
            assertThat(found.get().getRoles()).contains(Role.USER);
            assertThat(found.get().getEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should find admin user by username")
        void shouldFindAdminUserByUsername() {
            Optional<User> found = userRepository.findByUsername("adminuser");

            assertThat(found).isPresent();
            assertThat(found.get().getUsername()).isEqualTo("adminuser");
            assertThat(found.get().getRoles()).contains(Role.USER, Role.ADMIN);
            assertThat(found.get().getRoles()).hasSize(2);
        }

        @Test
        @DisplayName("Should find disabled user by username")
        void shouldFindDisabledUserByUsername() {
            Optional<User> found = userRepository.findByUsername("disableduser");

            assertThat(found).isPresent();
            assertThat(found.get().getUsername()).isEqualTo("disableduser");
            assertThat(found.get().getEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should return empty when username not found")
        void shouldReturnEmptyWhenUsernameNotFound() {
            Optional<User> found = userRepository.findByUsername("nonexistent");

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should return empty when username is null")
        void shouldReturnEmptyWhenUsernameIsNull() {
            Optional<User> found = userRepository.findByUsername(null);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should return empty when username is empty")
        void shouldReturnEmptyWhenUsernameIsEmpty() {
            Optional<User> found = userRepository.findByUsername("");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Find By Email Tests")
    class FindByEmailTests {

        @Test
        @DisplayName("Should find user by email")
        void shouldFindUserByEmail() {
            Optional<User> found = userRepository.findByEmail("test1@example.com");

            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("test1@example.com");
            assertThat(found.get().getUsername()).isEqualTo("testuser1");
        }

        @Test
        @DisplayName("Should find admin user by email")
        void shouldFindAdminUserByEmail() {
            Optional<User> found = userRepository.findByEmail("admin@example.com");

            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("admin@example.com");
            assertThat(found.get().getUsername()).isEqualTo("adminuser");
            assertThat(found.get().getRoles()).contains(Role.ADMIN);
        }

        @Test
        @DisplayName("Should return empty when email not found")
        void shouldReturnEmptyWhenEmailNotFound() {
            Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should return empty when email is null")
        void shouldReturnEmptyWhenEmailIsNull() {
            Optional<User> found = userRepository.findByEmail(null);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should return empty when email is empty")
        void shouldReturnEmptyWhenEmailIsEmpty() {
            Optional<User> found = userRepository.findByEmail("");

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Exists By Username Tests")
    class ExistsByUsernameTests {

        @Test
        @DisplayName("Should return true when username exists")
        void shouldReturnTrueWhenUsernameExists() {
            boolean exists = userRepository.existsByUsername("testuser1");

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return true when admin username exists")
        void shouldReturnTrueWhenAdminUsernameExists() {
            boolean exists = userRepository.existsByUsername("adminuser");

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false when username does not exist")
        void shouldReturnFalseWhenUsernameDoesNotExist() {
            boolean exists = userRepository.existsByUsername("nonexistent");

            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Should return false when username is null")
        void shouldReturnFalseWhenUsernameIsNull() {
            boolean exists = userRepository.existsByUsername(null);

            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Should return false when username is empty")
        void shouldReturnFalseWhenUsernameIsEmpty() {
            boolean exists = userRepository.existsByUsername("");

            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("Exists By Email Tests")
    class ExistsByEmailTests {

        @Test
        @DisplayName("Should return true when email exists")
        void shouldReturnTrueWhenEmailExists() {
            boolean exists = userRepository.existsByEmail("test1@example.com");

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return true when admin email exists")
        void shouldReturnTrueWhenAdminEmailExists() {
            boolean exists = userRepository.existsByEmail("admin@example.com");

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false when email does not exist")
        void shouldReturnFalseWhenEmailDoesNotExist() {
            boolean exists = userRepository.existsByEmail("nonexistent@example.com");

            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Should return false when email is null")
        void shouldReturnFalseWhenEmailIsNull() {
            boolean exists = userRepository.existsByEmail(null);

            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Should return false when email is empty")
        void shouldReturnFalseWhenEmailIsEmpty() {
            boolean exists = userRepository.existsByEmail("");

            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("CRUD Operations Tests")
    class CrudOperationsTests {

        @Test
        @DisplayName("Should save new user")
        void shouldSaveNewUser() {
            User newUser = User.builder()
                    .username("newuser")
                    .email("newuser@example.com")
                    .password("encodedPassword")
                    .roles(Set.of(Role.USER))
                    .enabled(true)
                    .build();

            User savedUser = userRepository.save(newUser);
            entityManager.flush();
            entityManager.clear();

            assertThat(savedUser.getId()).isNotNull();
            assertThat(savedUser.getUsername()).isEqualTo("newuser");
            assertThat(savedUser.getEmail()).isEqualTo("newuser@example.com");

            Optional<User> found = userRepository.findByUsername("newuser");
            assertThat(found).isPresent();
            assertThat(found.get().getUsername()).isEqualTo("newuser");
        }

        @Test
        @DisplayName("Should update existing user")
        void shouldUpdateExistingUser() {
            User userToUpdate = userRepository.findByUsername("testuser1").orElseThrow();
            userToUpdate.setEmail("updated@example.com");
            userToUpdate.setEnabled(false);

            User updatedUser = userRepository.save(userToUpdate);
            entityManager.flush();
            entityManager.clear();

            assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
            assertThat(updatedUser.getEnabled()).isFalse();

            Optional<User> found = userRepository.findByUsername("testuser1");
            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("updated@example.com");
            assertThat(found.get().getEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should delete user")
        void shouldDeleteUser() {
            User userToDelete = userRepository.findByUsername("testuser1").orElseThrow();
            Long userId = userToDelete.getId();

            userRepository.delete(userToDelete);
            entityManager.flush();
            entityManager.clear();

            Optional<User> found = userRepository.findById(userId);
            assertThat(found).isEmpty();

            Optional<User> foundByUsername = userRepository.findByUsername("testuser1");
            assertThat(foundByUsername).isEmpty();
        }

        @Test
        @DisplayName("Should count all users")
        void shouldCountAllUsers() {
            long count = userRepository.count();

            assertThat(count).isGreaterThanOrEqualTo(4);

            assertThat(userRepository.existsByUsername("testuser1")).isTrue();
            assertThat(userRepository.existsByUsername("testuser2")).isTrue();
            assertThat(userRepository.existsByUsername("adminuser")).isTrue();
            assertThat(userRepository.existsByUsername("disableduser")).isTrue();
        }
    }

    @Nested
    @DisplayName("Role Mapping Tests")
    class RoleMappingTests {

        @Test
        @DisplayName("Should persist user with single role")
        void shouldPersistUserWithSingleRole() {
            User singleRoleUser = User.builder()
                    .username("singlerole")
                    .email("singlerole@example.com")
                    .password("encodedPassword")
                    .roles(Set.of(Role.USER))
                    .enabled(true)
                    .build();

            userRepository.save(singleRoleUser);
            entityManager.flush();
            entityManager.clear();

            Optional<User> found = userRepository.findByUsername("singlerole");
            assertThat(found).isPresent();
            assertThat(found.get().getRoles()).hasSize(1);
            assertThat(found.get().getRoles()).contains(Role.USER);
        }

        @Test
        @DisplayName("Should persist user with multiple roles")
        void shouldPersistUserWithMultipleRoles() {
            User multiRoleUser = User.builder()
                    .username("multirole")
                    .email("multirole@example.com")
                    .password("encodedPassword")
                    .roles(Set.of(Role.USER, Role.ADMIN))
                    .enabled(true)
                    .build();

            userRepository.save(multiRoleUser);
            entityManager.flush();
            entityManager.clear();

            Optional<User> found = userRepository.findByUsername("multirole");
            assertThat(found).isPresent();
            assertThat(found.get().getRoles()).hasSize(2);
            assertThat(found.get().getRoles()).contains(Role.USER, Role.ADMIN);
        }

        @Test
        @DisplayName("Should persist user with no roles")
        void shouldPersistUserWithNoRoles() {
            User noRoleUser = User.builder()
                    .username("norole")
                    .email("norole@example.com")
                    .password("encodedPassword")
                    .roles(Set.of())
                    .enabled(true)
                    .build();

            userRepository.save(noRoleUser);
            entityManager.flush();
            entityManager.clear();

            Optional<User> found = userRepository.findByUsername("norole");
            assertThat(found).isPresent();
            assertThat(found.get().getRoles()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should reject username exceeding max length")
        void shouldRejectUsernameTooLong() {
            String tooLongUsername = "a".repeat(100);
            User invalidUser = User.builder()
                    .username(tooLongUsername)
                    .email("toolong@example.com")
                    .password("encodedPassword")
                    .roles(Set.of(Role.USER))
                    .enabled(true)
                    .build();

            org.junit.jupiter.api.Assertions.assertThrows(
                    org.springframework.dao.DataIntegrityViolationException.class,
                    () -> {
                        userRepository.save(invalidUser);
                        entityManager.flush();
                    }
            );
        }

        @Test
        @DisplayName("Should handle user with special characters in username")
        void shouldHandleUserWithSpecialCharactersInUsername() {
            String specialUsername = "user@domain.com";
            User specialUser = User.builder()
                    .username(specialUsername)
                    .email("special@example.com")
                    .password("encodedPassword")
                    .roles(Set.of(Role.USER))
                    .enabled(true)
                    .build();

            userRepository.save(specialUser);
            entityManager.flush();
            entityManager.clear();

            Optional<User> found = userRepository.findByUsername(specialUsername);
            assertThat(found).isPresent();
            assertThat(found.get().getUsername()).isEqualTo(specialUsername);
        }

        @Test
        @DisplayName("Should handle case sensitivity in username")
        void shouldHandleCaseSensitivityInUsername() {
            User caseUser = User.builder()
                    .username("CaseUser")
                    .email("case@example.com")
                    .password("encodedPassword")
                    .roles(Set.of(Role.USER))
                    .enabled(true)
                    .build();

            userRepository.save(caseUser);
            entityManager.flush();
            entityManager.clear();

            Optional<User> foundExact = userRepository.findByUsername("CaseUser");
            Optional<User> foundLower = userRepository.findByUsername("caseuser");
            Optional<User> foundUpper = userRepository.findByUsername("CASEUSER");

            assertThat(foundExact).isPresent();
            assertThat(foundLower).isEmpty();
            assertThat(foundUpper).isEmpty();
        }
    }
}
