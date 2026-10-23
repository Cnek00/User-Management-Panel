package com.example.UserManagmentPanel.api;

import com.example.UserManagmentPanel.api.dto.CreateUserRequest;
import com.example.UserManagmentPanel.api.dto.UpdateUserRequest;
import com.example.UserManagmentPanel.api.dto.UserResponse;
import com.example.UserManagmentPanel.entity.User;
import com.example.UserManagmentPanel.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User administration endpoints protected with HTTP Basic")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','MODERATOR')")
public class UserApiController {

    private final UserService userService;

    public UserApiController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List users", description = "Optionally filter by username or email.")
    public List<UserResponse> list(@RequestParam(required = false) String q) {
        return userService.search(q).stream().map(UserMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one user")
    @ApiResponse(responseCode = "404", description = "User not found")
    public UserResponse get(@PathVariable Long id) {
        return UserMapper.toResponse(userService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Create a user")
    @ApiResponse(responseCode = "201", description = "User created")
    @ApiResponse(responseCode = "409", description = "Username or email already exists")
    public UserResponse create(@Valid @RequestBody CreateUserRequest request, Authentication authentication) {
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(request.password());
        User created = userService.create(user, request.rankId(), request.roles(), authentication.getName());
        return UserMapper.toResponse(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Update a user")
    public UserResponse update(@PathVariable Long id,
                               @Valid @RequestBody UpdateUserRequest request,
                               Authentication authentication) {
        User updated = userService.update(
                id,
                request.email(),
                request.enabled(),
                request.rankId(),
                request.roles(),
                request.password(),
                authentication.getName()
        );
        return UserMapper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Delete a user")
    @ApiResponse(responseCode = "204", description = "User deleted")
    @ApiResponse(responseCode = "409", description = "The request violates a domain rule")
    public void delete(@PathVariable Long id, Authentication authentication) {
        userService.delete(id, authentication.getName());
    }
}
