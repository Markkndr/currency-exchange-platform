package com.currencyexchange.service;

import com.currencyexchange.dto.auth.*;
import com.currencyexchange.entity.User;
import com.currencyexchange.exception.*;
import com.currencyexchange.repository.UserRepository;
import com.currencyexchange.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setId(42L);
        activeUser.setEmail("alice@example.com");
        activeUser.setPassword("encoded-current");
        activeUser.setFullName("Alice Example");
        activeUser.setIsActive(true);
    }

    @Nested
    @DisplayName("register")
    class Register {

        private RegisterRequestDTO request() {
            return new RegisterRequestDTO(
                    "alice@example.com", "plain-password", "Alice Example", "+123456789", "US");
        }

        @Test
        @DisplayName("persists a new user, encodes the password and returns tokens")
        void registersNewUser() {
            RegisterRequestDTO request = request();
            when(userService.userExistsByEmail("alice@example.com")).thenReturn(false);
            when(passwordEncoder.encode("plain-password")).thenReturn("encoded-password");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User toSave = invocation.getArgument(0);
                toSave.setId(42L);
                return toSave;
            });
            when(tokenProvider.generateAccessToken("alice@example.com", 42L)).thenReturn("access-token");
            when(tokenProvider.generateRefreshToken("alice@example.com", 42L)).thenReturn("refresh-token");
            when(tokenProvider.getRemainingTime("access-token")).thenReturn(3600L);

            AuthResponseDTO response = authService.register(request);

            assertThat(response.getUserId()).isEqualTo(42L);
            assertThat(response.getEmail()).isEqualTo("alice@example.com");
            assertThat(response.getFullName()).isEqualTo("Alice Example");
            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getExpiresIn()).isEqualTo(3600L);

            ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(savedUser.capture());
            User persisted = savedUser.getValue();
            assertThat(persisted.getPassword()).isEqualTo("encoded-password");
            assertThat(persisted.getIsActive()).isTrue();
            assertThat(persisted.getIsEmailVerified()).isFalse();
            assertThat(persisted.getEmailVerificationToken()).isNotBlank();
            assertThat(persisted.getEmailVerificationTokenExpiry()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("rejects an email that already exists")
        void rejectsDuplicateEmail() {
            when(userService.userExistsByEmail("alice@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request()))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("alice@example.com");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        private LoginRequestDTO request() {
            return new LoginRequestDTO("alice@example.com", "plain-password");
        }

        @Test
        @DisplayName("authenticates, updates last-login and returns tokens")
        void loginsSuccessfully() {
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken("alice@example.com", "plain-password");
            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            when(userService.getUserByEmail("alice@example.com")).thenReturn(activeUser);
            when(tokenProvider.generateAccessToken("alice@example.com", 42L)).thenReturn("access-token");
            when(tokenProvider.generateRefreshToken("alice@example.com", 42L)).thenReturn("refresh-token");
            when(tokenProvider.getRemainingTime("access-token")).thenReturn(3600L);

            AuthResponseDTO response = authService.login(request());

            assertThat(response.getUserId()).isEqualTo(42L);
            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            verify(userService).updateLastLoginTime(42L);
        }

        @Test
        @DisplayName("flattens a Spring credential failure into a generic AuthenticationException")
        void rejectsBadCredentials() {
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("bad creds"));

            assertThatThrownBy(() -> authService.login(request()))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessage("Invalid email or password");

            verify(userService, never()).updateLastLoginTime(any());
        }

        @Test
        @DisplayName("rejects a disabled account")
        void rejectsDisabledAccount() {
            activeUser.setIsActive(false);
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken("alice@example.com", "plain-password");
            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            when(userService.getUserByEmail("alice@example.com")).thenReturn(activeUser);

            assertThatThrownBy(() -> authService.login(request()))
                    .isInstanceOf(AccountDisabledException.class);

            verify(userService, never()).updateLastLoginTime(any());
        }
    }

    @Nested
    @DisplayName("refreshAccessToken")
    class RefreshAccessToken {

        @Test
        @DisplayName("issues a new access token for a valid refresh token")
        void refreshesSuccessfully() {
            RefreshTokenRequestDTO request = new RefreshTokenRequestDTO("refresh-token");
            when(tokenProvider.validateToken("refresh-token")).thenReturn(true);
            when(tokenProvider.getTokenType("refresh-token")).thenReturn("REFRESH");
            when(tokenProvider.getUsernameFromToken("refresh-token")).thenReturn("alice@example.com");
            when(tokenProvider.getUserIdFromToken("refresh-token")).thenReturn(42L);
            when(userService.getUserByEmail("alice@example.com")).thenReturn(activeUser);
            when(tokenProvider.generateAccessToken("alice@example.com", 42L)).thenReturn("new-access-token");
            when(tokenProvider.getRemainingTime("new-access-token")).thenReturn(3600L);

            RefreshTokenResponseDTO response = authService.refreshAccessToken(request);

            assertThat(response.getAccessToken()).isEqualTo("new-access-token");
            assertThat(response.getExpiresIn()).isEqualTo(3600L);
        }

        @Test
        @DisplayName("rejects an invalid refresh token")
        void rejectsInvalidToken() {
            RefreshTokenRequestDTO request = new RefreshTokenRequestDTO("bad-token");
            when(tokenProvider.validateToken("bad-token")).thenReturn(false);

            assertThatThrownBy(() -> authService.refreshAccessToken(request))
                    .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("rejects an access token presented as a refresh token")
        void rejectsWrongTokenType() {
            RefreshTokenRequestDTO request = new RefreshTokenRequestDTO("access-token");
            when(tokenProvider.validateToken("access-token")).thenReturn(true);
            when(tokenProvider.getTokenType("access-token")).thenReturn("ACCESS");

            assertThatThrownBy(() -> authService.refreshAccessToken(request))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("not a refresh token");
        }

        @Test
        @DisplayName("rejects refresh for a disabled account")
        void rejectsDisabledAccount() {
            activeUser.setIsActive(false);
            RefreshTokenRequestDTO request = new RefreshTokenRequestDTO("refresh-token");
            when(tokenProvider.validateToken("refresh-token")).thenReturn(true);
            when(tokenProvider.getTokenType("refresh-token")).thenReturn("REFRESH");
            when(tokenProvider.getUsernameFromToken("refresh-token")).thenReturn("alice@example.com");
            when(tokenProvider.getUserIdFromToken("refresh-token")).thenReturn(42L);
            when(userService.getUserByEmail("alice@example.com")).thenReturn(activeUser);

            assertThatThrownBy(() -> authService.refreshAccessToken(request))
                    .isInstanceOf(AccountDisabledException.class);
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("encodes and stores a valid new password")
        void changesPassword() {
            ChangePasswordRequestDTO request =
                    new ChangePasswordRequestDTO("current", "new-pass", "new-pass");
            when(userService.getUserById(42L)).thenReturn(activeUser);
            when(passwordEncoder.matches("current", "encoded-current")).thenReturn(true);
            when(passwordEncoder.matches("new-pass", "encoded-current")).thenReturn(false);
            when(passwordEncoder.encode("new-pass")).thenReturn("encoded-new");

            authService.changePassword(42L, request);

            assertThat(activeUser.getPassword()).isEqualTo("encoded-new");
            verify(userRepository).save(activeUser);
        }

        @Test
        @DisplayName("rejects a wrong current password")
        void rejectsWrongCurrentPassword() {
            ChangePasswordRequestDTO request =
                    new ChangePasswordRequestDTO("wrong", "new-pass", "new-pass");
            when(userService.getUserById(42L)).thenReturn(activeUser);
            when(passwordEncoder.matches("wrong", "encoded-current")).thenReturn(false);

            assertThatThrownBy(() -> authService.changePassword(42L, request))
                    .isInstanceOf(InvalidPasswordException.class)
                    .hasMessageContaining("Current password");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects a confirmation that does not match")
        void rejectsMismatchedConfirmation() {
            ChangePasswordRequestDTO request =
                    new ChangePasswordRequestDTO("current", "new-pass", "different");
            when(userService.getUserById(42L)).thenReturn(activeUser);
            when(passwordEncoder.matches("current", "encoded-current")).thenReturn(true);

            assertThatThrownBy(() -> authService.changePassword(42L, request))
                    .isInstanceOf(InvalidPasswordException.class)
                    .hasMessageContaining("do not match");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects reusing the current password")
        void rejectsSamePassword() {
            ChangePasswordRequestDTO request =
                    new ChangePasswordRequestDTO("current", "current", "current");
            when(userService.getUserById(42L)).thenReturn(activeUser);
            when(passwordEncoder.matches("current", "encoded-current")).thenReturn(true);

            assertThatThrownBy(() -> authService.changePassword(42L, request))
                    .isInstanceOf(InvalidPasswordException.class)
                    .hasMessageContaining("cannot be the same");

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("verifyEmail")
    class VerifyEmail {

        @Test
        @DisplayName("verifies a user for a valid, unexpired token")
        void verifiesEmail() {
            activeUser.setEmailVerificationToken("verify-token");
            activeUser.setEmailVerificationTokenExpiry(LocalDateTime.now().plusHours(1));
            when(userRepository.findByEmailVerificationToken("verify-token"))
                    .thenReturn(Optional.of(activeUser));

            authService.verifyEmail("verify-token");

            verify(userService).verifyEmail(42L);
        }

        @Test
        @DisplayName("rejects an unknown verification token")
        void rejectsUnknownToken() {
            when(userRepository.findByEmailVerificationToken("nope"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.verifyEmail("nope"))
                    .isInstanceOf(InvalidTokenException.class);

            verify(userService, never()).verifyEmail(any());
        }

        @Test
        @DisplayName("rejects an expired verification token")
        void rejectsExpiredToken() {
            activeUser.setEmailVerificationToken("verify-token");
            activeUser.setEmailVerificationTokenExpiry(LocalDateTime.now().minusHours(1));
            when(userRepository.findByEmailVerificationToken("verify-token"))
                    .thenReturn(Optional.of(activeUser));

            assertThatThrownBy(() -> authService.verifyEmail("verify-token"))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("expired");

            verify(userService, never()).verifyEmail(any());
        }
    }

    @Test
    @DisplayName("logout is a no-op for stateless JWT (no exception, no persistence)")
    void logoutIsNoOp() {
        authService.logout(42L);
        verify(userRepository, never()).save(any());
    }
}
