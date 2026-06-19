package com.currencyexchange.service;

import com.currencyexchange.entity.User;
import com.currencyexchange.exception.UserNotFoundException;
import com.currencyexchange.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(7L);
        user.setEmail("bob@example.com");
        user.setPassword("encoded-password");
        user.setIsActive(true);
        user.setIsEmailVerified(false);
    }

    @Test
    @DisplayName("loadUserByUsername returns Spring UserDetails for an active user")
    void loadsActiveUser() {
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(user));

        UserDetails details = userService.loadUserByUsername("bob@example.com");

        assertThat(details.getUsername()).isEqualTo("bob@example.com");
        assertThat(details.getPassword()).isEqualTo("encoded-password");
        assertThat(details.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("loadUserByUsername throws when the email is unknown")
    void loadFailsForUnknownEmail() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername("ghost@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("loadUserByUsername throws when the account is disabled")
    void loadFailsForDisabledAccount() {
        user.setIsActive(false);
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.loadUserByUsername("bob@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    @DisplayName("getUserById returns the user when present")
    void getsUserById() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        assertThat(userService.getUserById(7L)).isSameAs(user);
    }

    @Test
    @DisplayName("getUserById throws when missing")
    void getUserByIdThrows() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("getUserByEmail returns the user when present")
    void getsUserByEmail() {
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(user));

        assertThat(userService.getUserByEmail("bob@example.com")).isSameAs(user);
    }

    @Test
    @DisplayName("getUserByEmail throws when missing")
    void getUserByEmailThrows() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByEmail("ghost@example.com"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("userExistsByEmail delegates to the repository")
    void delegatesExistsByEmail() {
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(true);

        assertThat(userService.userExistsByEmail("bob@example.com")).isTrue();
    }

    @Test
    @DisplayName("updateLastLoginTime stamps lastLogin and saves")
    void updatesLastLoginTime() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        userService.updateLastLoginTime(7L);

        assertThat(user.getLastLogin()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("verifyEmail marks the user verified and clears the token")
    void verifiesEmail() {
        user.setEmailVerificationToken("token");
        user.setEmailVerificationTokenExpiry(java.time.LocalDateTime.now().plusHours(1));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        userService.verifyEmail(7L);

        assertThat(user.getIsEmailVerified()).isTrue();
        assertThat(user.getEmailVerificationToken()).isNull();
        assertThat(user.getEmailVerificationTokenExpiry()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("activateUser sets the account active and saves")
    void activatesUser() {
        user.setIsActive(false);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        userService.activateUser(7L);

        assertThat(user.getIsActive()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("deactivateUser sets the account inactive and saves")
    void deactivatesUser() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        userService.deactivateUser(7L);

        assertThat(user.getIsActive()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("enableTwoFactor stores the secret and enables 2FA")
    void enablesTwoFactor() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        userService.enableTwoFactor(7L, "secret-key");

        assertThat(user.getTwoFactorEnabled()).isTrue();
        assertThat(user.getTwoFactorSecret()).isEqualTo("secret-key");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("disableTwoFactor clears the secret and disables 2FA")
    void disablesTwoFactor() {
        user.setTwoFactorEnabled(true);
        user.setTwoFactorSecret("secret-key");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        userService.disableTwoFactor(7L);

        assertThat(user.getTwoFactorEnabled()).isFalse();
        assertThat(user.getTwoFactorSecret()).isNull();
        verify(userRepository).save(user);
    }
}
