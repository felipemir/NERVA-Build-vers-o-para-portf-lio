package com.nervatech.nerva_build.service;

import com.nervatech.nerva_build.domain.model.AppUser;
import com.nervatech.nerva_build.dto.LoginRequestDTO;
import com.nervatech.nerva_build.dto.LoginResponseDTO;
import com.nervatech.nerva_build.dto.MeResponseDTO;
import com.nervatech.nerva_build.dto.RegisterRequestDTO;
import com.nervatech.nerva_build.dto.RegisterResponseDTO;
import com.nervatech.nerva_build.repository.AppUserRepository;
import com.nervatech.nerva_build.security.AppUserDetails;
import com.nervatech.nerva_build.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;

@Service
public class AuthService {

    private static final Set<String> CREATABLE_BY_OWNER = Set.of("STORE_ADMIN", "STORE_OPERATOR");
    private static final Set<String> CREATABLE_BY_ADMIN = Set.of("STORE_OPERATOR");

    private final AppUserAuthService appUserAuthService;
    private final AppUserRepository appUserRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            AppUserAuthService appUserAuthService,
            AppUserRepository appUserRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder
    ) {
        this.appUserAuthService = appUserAuthService;
        this.appUserRepository = appUserRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO request) {
        AppUser user = appUserAuthService.findByEmailAndStoreOrThrow(request.email(), request.storeId());

        if (!passwordMatches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
        }

        if (!isBcrypt(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.password()));
            appUserAuthService.save(user);
        }

        String token = jwtService.generateToken(user);

        return new LoginResponseDTO(
                token,
                "Bearer",
                jwtService.getExpirationMs(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getStoreId()
        );
    }

    @Transactional(readOnly = true)
    public MeResponseDTO me(AppUserDetails currentUser) {
        return new MeResponseDTO(
                currentUser.getId(),
                currentUser.getDisplayName(),
                currentUser.getUsername(),
                currentUser.getRole(),
                currentUser.getStoreId()
        );
    }

    @Transactional
    public RegisterResponseDTO register(RegisterRequestDTO request, AppUserDetails currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não autenticado");
        }

        if (request.storeId() == null || !request.storeId().equals(currentUser.getStoreId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão para cadastrar usuário em outra loja");
        }

        String creatorRole = normalizeRole(currentUser.getRole());
        String targetRole = normalizeRole(request.role());

        validateCreatePermission(creatorRole, targetRole);

        if (isBlank(request.name()) || isBlank(request.email()) || isBlank(request.password())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome, email e senha são obrigatórios");
        }

        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (appUserRepository.existsByEmailAndStoreId(normalizedEmail, request.storeId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe usuário com este email nessa loja");
        }

        AppUser user = new AppUser();
        user.setStoreId(request.storeId());
        user.setName(request.name().trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(targetRole);
        user.setActive(true);

        AppUser saved = appUserAuthService.save(user);

        return new RegisterResponseDTO(
                saved.getId(),
                saved.getName(),
                saved.getEmail(),
                saved.getRole(),
                saved.getStoreId(),
                saved.getActive()
        );
    }

    private void validateCreatePermission(String creatorRole, String targetRole) {
        if ("STORE_OWNER".equals(creatorRole) && CREATABLE_BY_OWNER.contains(targetRole)) {
            return;
        }

        if ("STORE_ADMIN".equals(creatorRole) && CREATABLE_BY_ADMIN.contains(targetRole)) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sem permissão para cadastrar esse tipo de usuário");
    }

    private String normalizeRole(String role) {
        if (isBlank(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role é obrigatória");
        }
        return role.trim().toUpperCase(Locale.ROOT);
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (!isBcrypt(storedPassword)) {
            return rawPassword != null && rawPassword.equals(storedPassword);
        }
        return passwordEncoder.matches(rawPassword, storedPassword);
    }

    private boolean isBcrypt(String password) {
        return password != null && (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$"));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
