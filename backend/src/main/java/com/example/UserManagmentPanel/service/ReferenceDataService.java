package com.example.UserManagmentPanel.service;

import com.example.UserManagmentPanel.entity.Permission;
import com.example.UserManagmentPanel.entity.Rank;
import com.example.UserManagmentPanel.entity.Role;
import com.example.UserManagmentPanel.entity.Scope;
import com.example.UserManagmentPanel.repository.PermissionRepository;
import com.example.UserManagmentPanel.repository.RankRepository;
import com.example.UserManagmentPanel.repository.RoleRepository;
import com.example.UserManagmentPanel.repository.ScopeRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Read-only reference data used by the forms and the REST metadata endpoint.
 */
@Service
@Transactional(readOnly = true)
public class ReferenceDataService {

    private final RankRepository rankRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final ScopeRepository scopeRepository;

    public ReferenceDataService(RankRepository rankRepository,
                                RoleRepository roleRepository,
                                PermissionRepository permissionRepository,
                                ScopeRepository scopeRepository) {
        this.rankRepository = rankRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.scopeRepository = scopeRepository;
    }

    public List<Rank> ranks() {
        return rankRepository.findAll(Sort.by(Sort.Direction.DESC, "level"));
    }

    public List<Role> roles() {
        return roleRepository.findAll(Sort.by("name"));
    }

    public List<Permission> permissions() {
        return permissionRepository.findAll(Sort.by("action"));
    }

    public List<Scope> scopes() {
        return scopeRepository.findAll(Sort.by("scopeKey"));
    }
}
