package com.akif.auth.web;

import com.akif.auth.api.*;
import com.akif.auth.internal.dto.AddNoteRequest;
import com.akif.auth.internal.dto.BanUserRequest;
import com.akif.auth.internal.dto.UnbanUserRequest;
import com.akif.rental.api.RentalResponse;
import com.akif.rental.api.RentalService;
import com.akif.rental.domain.enums.RentalStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin User Management", description = "User management operations for administrators")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final RentalService rentalService;
    private final AuthService authService;

    @GetMapping("/stats")
    @Operation(summary = "Get user statistics (Admin)", description = "Returns user count statistics for dashboard")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public ResponseEntity<UserStatsResponse> getStats(
            @AuthenticationPrincipal UserDetails adminUser) {

        log.info("GET /api/admin/users/stats - Admin: {}", adminUser.getUsername());

        UserStatsResponse stats = adminUserService.getStats();

        return ResponseEntity.ok(stats);
    }

    @GetMapping
    @Operation(summary = "List all users (Admin)", description = "Returns paginated list of users with optional filters")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users list retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public ResponseEntity<Page<AdminUserListItem>> getAllUsers(
            @Parameter(description = "Filter by role (ADMIN, USER)") @RequestParam(required = false) String role,
            @Parameter(description = "Filter by status (ACTIVE, BANNED)") @RequestParam(required = false) String status,
            @Parameter(description = "Search by name or email") @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createTime", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserDetails adminUser) {

        log.info("GET /api/admin/users - Admin: {}, Role: {}, Status: {}, Search: {}",
                adminUser.getUsername(), role, status, search);

        Page<AdminUserListItem> users = adminUserService.getAllUsers(role, status, search, pageable);

        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user details (Admin)", description = "Returns comprehensive user information with statistics for admin panel")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User details retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<AdminUserDetailResponse> getUserDetail(
            @Parameter(description = "User ID", required = true) @PathVariable Long id,
            @AuthenticationPrincipal UserDetails adminUser) {

        log.info("GET /api/admin/users/{} - Admin: {}", id, adminUser.getUsername());

        AdminUserDetailResponse userDetail = adminUserService.getUserDetailForAdmin(id);

        return ResponseEntity.ok(userDetail);
    }

    @GetMapping("/{id}/rentals")
    @Operation(summary = "Get user rental history (Admin)", description = "Returns paginated rental history for a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rental history retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Page<RentalResponse>> getUserRentals(
            @Parameter(description = "User ID", required = true) @PathVariable Long id,
            @Parameter(description = "Filter by rental status") @RequestParam(required = false) RentalStatus status,
            @PageableDefault(size = 10, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserDetails adminUser) {

        log.info("GET /api/admin/users/{}/rentals - Admin: {}, Status: {}", id, adminUser.getUsername(),
                status);

        adminUserService.getUserDetailForAdmin(id);

        Page<RentalResponse> rentals = rentalService.getUserRentals(id, status, pageable);

        return ResponseEntity.ok(rentals);
    }

    @PostMapping("/{id}/ban")
    @Operation(summary = "Ban user (Admin)", description = "Bans a user account with a reason")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User banned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or user already banned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Map<String, String>> banUser(
            @Parameter(description = "User ID", required = true) @PathVariable Long id,
            @Valid @RequestBody BanUserRequest request,
            @AuthenticationPrincipal UserDetails adminUser) {

        log.info("POST /api/admin/users/{}/ban - Admin: {}", id, adminUser.getUsername());

        UserDto admin = authService.getUserByUsername(adminUser.getUsername());
        adminUserService.banUser(id, request.reason(), admin.id());

        return ResponseEntity.ok(Map.of("message", "User banned successfully"));
    }

    @PostMapping("/{id}/unban")
    @Operation(summary = "Unban user (Admin)", description = "Unbans a previously banned user account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User unbanned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or user not banned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Map<String, String>> unbanUser(
            @Parameter(description = "User ID", required = true) @PathVariable Long id,
            @Valid @RequestBody(required = false) UnbanUserRequest request,
            @AuthenticationPrincipal UserDetails adminUser) {

        log.info("POST /api/admin/users/{}/unban - Admin: {}", id, adminUser.getUsername());

        UserDto admin = authService.getUserByUsername(adminUser.getUsername());
        String note = request != null ? request.note() : null;
        adminUserService.unbanUser(id, note, admin.id());

        return ResponseEntity.ok(Map.of("message", "User unbanned successfully"));
    }

    @PostMapping("/{id}/notes")
    @Operation(summary = "Add admin note (Admin)", description = "Adds an internal note to a user's profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Note added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<AdminNoteDto> addNote(
            @Parameter(description = "User ID", required = true) @PathVariable Long id,
            @Valid @RequestBody AddNoteRequest request,
            @AuthenticationPrincipal UserDetails adminUser) {

        log.info("POST /api/admin/users/{}/notes - Admin: {}", id, adminUser.getUsername());

        UserDto admin = authService.getUserByUsername(adminUser.getUsername());
        AdminNoteDto note = adminUserService.addAdminNote(id, request.text(), admin.id(),
                admin.username());

        return ResponseEntity.status(201).body(note);
    }

    @GetMapping("/{id}/notes")
    @Operation(summary = "Get admin notes (Admin)", description = "Returns all internal notes for a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notes retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<List<AdminNoteDto>> getNotes(
            @Parameter(description = "User ID", required = true) @PathVariable Long id,
            @AuthenticationPrincipal UserDetails adminUser) {

        log.info("GET /api/admin/users/{}/notes - Admin: {}", id, adminUser.getUsername());

        List<AdminNoteDto> notes = adminUserService.getAdminNotes(id);

        return ResponseEntity.ok(notes);
    }
}