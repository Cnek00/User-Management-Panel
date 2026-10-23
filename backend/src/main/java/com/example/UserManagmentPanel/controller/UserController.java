package com.example.UserManagmentPanel.controller;

import com.example.UserManagmentPanel.entity.RoleType;
import com.example.UserManagmentPanel.entity.User;
import com.example.UserManagmentPanel.exception.DomainRuleException;
import com.example.UserManagmentPanel.exception.DuplicateResourceException;
import com.example.UserManagmentPanel.service.PermissionService;
import com.example.UserManagmentPanel.service.ReferenceDataService;
import com.example.UserManagmentPanel.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

@Controller
@RequestMapping("/users")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','MODERATOR')")
public class UserController {

    private final UserService userService;
    private final PermissionService permissionService;
    private final ReferenceDataService referenceDataService;

    public UserController(UserService userService,
                          PermissionService permissionService,
                          ReferenceDataService referenceDataService) {
        this.userService = userService;
        this.permissionService = permissionService;
        this.referenceDataService = referenceDataService;
    }

    @GetMapping
    public String listUsers(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("users", userService.search(q));
        model.addAttribute("query", q == null ? "" : q);
        return "user-list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new User());
        addFormReferenceData(model);
        return "register";
    }

    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public String saveUser(@Valid @ModelAttribute("user") User user,
                           BindingResult bindingResult,
                           @RequestParam(required = false) Long rankId,
                           @RequestParam(required = false) Set<RoleType> roles,
                           Authentication authentication,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormReferenceData(model);
            return "register";
        }

        try {
            userService.create(user, rankId, roles, authentication.getName());
        } catch (DuplicateResourceException | DomainRuleException | AccessDeniedException exception) {
            model.addAttribute("formError", exception.getMessage());
            addFormReferenceData(model);
            return "register";
        }

        redirectAttributes.addFlashAttribute("successMessage", "User created successfully.");
        return "redirect:/users";
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public String toggleUser(@PathVariable Long id,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        return runAction(redirectAttributes, "User status updated.",
                () -> userService.toggleEnabled(id, authentication.getName()));
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public String deleteUser(@PathVariable Long id,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        return runAction(redirectAttributes, "User deleted.",
                () -> userService.delete(id, authentication.getName()));
    }

    @GetMapping("/{id}/permissions")
    public String showPermissionPage(@PathVariable Long id, Model model) {
        model.addAttribute("user", userService.findById(id));
        model.addAttribute("allRanks", referenceDataService.ranks());
        model.addAttribute("allPermissions", referenceDataService.permissions());
        model.addAttribute("allScopes", referenceDataService.scopes());
        model.addAttribute("userPermissions", permissionService.findByUserId(id));
        return "user-permissions";
    }

    @PostMapping("/{id}/update-rank")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public String updateRank(@PathVariable Long id,
                             @RequestParam Long rankId,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        return runAction(redirectAttributes, "Rank updated.",
                () -> userService.updateRank(id, rankId, authentication.getName()),
                "redirect:/users/" + id + "/permissions");
    }

    @PostMapping("/{id}/grant-permission")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public String grantPermission(@PathVariable Long id,
                                  @RequestParam Long permissionId,
                                  @RequestParam Long scopeId,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        return runAction(redirectAttributes, "Permission granted.",
                () -> permissionService.grant(id, permissionId, scopeId, authentication.getName()),
                "redirect:/users/" + id + "/permissions");
    }

    @PostMapping("/{id}/revoke-permission/{userPermissionId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public String revokePermission(@PathVariable Long id,
                                   @PathVariable Long userPermissionId,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        return runAction(redirectAttributes, "Permission revoked.",
                () -> permissionService.revoke(id, userPermissionId, authentication.getName()),
                "redirect:/users/" + id + "/permissions");
    }

    private String runAction(RedirectAttributes redirectAttributes, String successMessage, Runnable action) {
        return runAction(redirectAttributes, successMessage, action, "redirect:/users");
    }

    private String runAction(RedirectAttributes redirectAttributes,
                             String successMessage,
                             Runnable action,
                             String redirectUrl) {
        try {
            action.run();
            redirectAttributes.addFlashAttribute("successMessage", successMessage);
        } catch (DuplicateResourceException | DomainRuleException | AccessDeniedException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return redirectUrl;
    }

    private void addFormReferenceData(Model model) {
        model.addAttribute("allRanks", referenceDataService.ranks());
        model.addAttribute("allRoles", referenceDataService.roles());
    }
}
