package com.akif.auth.web;

import com.akif.auth.api.AdminUserService;
import com.akif.auth.api.AdminUserDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin User Management", description = "User management operations for administrators")
public class AdminUserController {

    private final AdminUserService adminUserService;

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
}
