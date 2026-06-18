package com.currencyexchange.service;

import com.currencyexchange.dto.auth.*;
import com.currencyexchange.entity.User;
import com.currencyexchange.exception.*;
import com.currencyexchange.repository.UserRepository;
import com.currencyexchange.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        if (userService.userExistsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User already exists with email: " + request.getEmail());
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setCountry(request.getCountry());
        user.setIsActive(true);
        user.setIsEmailVerified(false);
        user.setEmailVerificationToken(UUID.randomUUID().toString());
        user.setEmailVerificationTokenExpiry(LocalDateTime.now().plusHours(24));

        user = userRepository.save(user);

        String accessToken = tokenProvider.generateAccessToken(user.getEmail(), user.getId());
        String refreshToken = tokenProvider.generateRefreshToken(user.getEmail(), user.getId());

        log.info("New user registered with email: {}", user.getEmail());

        return AuthResponseDTO.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(tokenProvider.getRemainingTime(accessToken))
                .build();
    }

    @Transactional
    public AuthResponseDTO login(LoginRequestDTO request) {
        // Only credential failures should be flattened into a generic message.
        // Anything else (disabled account, lookup/DB errors) must surface as-is.
        String email;
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
            email = authentication.getName();
        } catch (org.springframework.security.core.AuthenticationException ex) {
            log.warn("Login failed for {}: {}", request.getEmail(), ex.getMessage());
            throw new AuthenticationException("Invalid email or password");
        }

        User user = userService.getUserByEmail(email);

        if (!user.getIsActive()) {
            throw new AccountDisabledException("Account is disabled");
        }

        // Email verification is not yet enforced because no email-delivery channel
        // exists to send the verification link. Enforce here (throwing
        // EmailNotVerifiedException) once delivery is wired up.

        userService.updateLastLoginTime(user.getId());

        String accessToken = tokenProvider.generateAccessToken(user.getEmail(), user.getId());
        String refreshToken = tokenProvider.generateRefreshToken(user.getEmail(), user.getId());

        log.info("User logged in successfully: {}", email);

        return AuthResponseDTO.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(tokenProvider.getRemainingTime(accessToken))
                .build();
    }

    @Transactional
    public RefreshTokenResponseDTO refreshAccessToken(RefreshTokenRequestDTO request) {
        String refreshToken = request.getRefreshToken();

        if (!tokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("Invalid or expired refresh token");
        }

        String tokenType = tokenProvider.getTokenType(refreshToken);
        if (!"REFRESH".equals(tokenType)) {
            throw new InvalidTokenException("Token is not a refresh token");
        }

        String email = tokenProvider.getUsernameFromToken(refreshToken);
        Long userId = tokenProvider.getUserIdFromToken(refreshToken);

        User user = userService.getUserByEmail(email);
        if (!user.getIsActive()) {
            throw new AccountDisabledException("Account is disabled");
        }

        String newAccessToken = tokenProvider.generateAccessToken(email, userId);

        log.info("Access token refreshed for user: {}", email);

        return RefreshTokenResponseDTO.builder()
                .accessToken(newAccessToken)
                .expiresIn(tokenProvider.getRemainingTime(newAccessToken))
                .build();
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequestDTO request) {
        User user = userService.getUserById(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new InvalidPasswordException("New password and confirm password do not match");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new InvalidPasswordException("New password cannot be the same as current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed for user: {}", userId);
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));

        if (user.getEmailVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Verification token has expired");
        }

        userService.verifyEmail(user.getId());
        log.info("Email verified for user: {}", user.getId());
    }
    // Stateless JWT: there is no server-side session to invalidate, so logout is a
    // no-op beyond audit logging. To support real revocation, persist a token
    // blacklist (e.g. by jti) and check it in JwtAuthenticationFilter.
    public void logout(Long userId) {
        log.info("User logged out: {}", userId);
    }
}