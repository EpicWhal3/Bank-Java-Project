package org.gateway.application.services;

import org.gateway.application.interfaces.AdminApi;
import org.gateway.infrastructure.entities.GatewayUser;
import org.gateway.infrastructure.entities.enums.Role;
import org.gateway.infrastructure.repos.GatewayUserRepository;
import org.gateway.infrastructure.requestEntities.CreateAdminRequest;
import org.gateway.infrastructure.requestEntities.CreateUserRequest;
import org.gateway.infrastructure.DTO.GatewayAccountDTO;
import org.gateway.infrastructure.DTO.GatewayUserDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServices {
    private final GatewayUserRepository gatewayUserRepository;
    private final PasswordEncoder passwordEncoder;

    private final AdminApi adminApi;

    @Transactional
    public void createAdmin(CreateAdminRequest admin) {
        GatewayUser gatewayUser = new GatewayUser(admin.getLogin(),
                passwordEncoder.encode(admin.getPassword()), List.of(Role.ADMIN));
        gatewayUserRepository.save(gatewayUser);
    }

    @Transactional
    public void createClient(CreateUserRequest clientRequest, String password, Authentication auth) {
        GatewayUser entity = new GatewayUser(clientRequest.getLogin(),
                passwordEncoder.encode(password), List.of(Role.CLIENT));
        gatewayUserRepository.save(entity);
        adminApi.createClient(clientRequest, password, auth);
    }

    public List<GatewayUserDTO> getAllUsers(Authentication auth) {
        return adminApi.getAllUsers(auth);
    }

    public List<GatewayUserDTO> getAllUsersGenderFilter(String gender, Authentication auth) {
        return adminApi.getAllUsersGenderFilter(gender, auth);
    }

    public List<GatewayUserDTO> getAllUsersHairColorFilter(String haircolor, Authentication auth) {
        return adminApi.getAllUsersHairColorFilter(haircolor, auth);
    }

    public GatewayUserDTO getUserById(long id, Authentication auth) {
        return adminApi.getUserById(id, auth);
    }

    public List<GatewayAccountDTO> getAllAccounts(Authentication auth) {
        return adminApi.getAllAccounts(auth);
    }

    public List<GatewayAccountDTO> getAllUserAccounts(long user_id, Authentication auth) {
        return adminApi.getAllUserAccounts(user_id, auth);
    }

    public GatewayAccountDTO getAccountById(long accountId, Authentication auth) {
        return adminApi.getAccountById(accountId, auth);
    }
}
