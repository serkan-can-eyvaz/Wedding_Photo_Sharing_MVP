package com.weddingshare.auth;

import com.weddingshare.user.User;
import com.weddingshare.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTests {

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String ADMIN_PASSWORD = "test-admin-password";

    @Mock
    private UserRepository userRepository;

    private final SecurityConfiguration securityConfiguration = new SecurityConfiguration();
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @Test
    void emptyUsersTableCreatesBootstrapAdmin() throws Exception {
        when(userRepository.count()).thenReturn(0L);

        bootstrap(ADMIN_EMAIL, ADMIN_PASSWORD).run(null);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo(ADMIN_EMAIL);
        assertThat(passwordEncoder.matches(ADMIN_PASSWORD, userCaptor.getValue().getPasswordHash())).isTrue();
    }

    @Test
    void existingConfiguredAdminIsNotChanged() throws Exception {
        when(userRepository.count()).thenReturn(1L);

        bootstrap(ADMIN_EMAIL, "new-password").run(null);

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void existingDifferentUserDoesNotCreateSecondAdminWhenConfiguredEmailChanges() throws Exception {
        when(userRepository.count()).thenReturn(1L);

        bootstrap("different-admin@example.com", ADMIN_PASSWORD).run(null);

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void repeatedBootstrapIsIdempotentAfterFirstUserIsCreated() throws Exception {
        AtomicLong userCount = new AtomicLong();
        when(userRepository.count()).thenAnswer(invocation -> userCount.get());
        doAnswer(invocation -> {
            userCount.incrementAndGet();
            return invocation.getArgument(0);
        }).when(userRepository).save(org.mockito.ArgumentMatchers.any(User.class));

        ApplicationRunner bootstrap = bootstrap(ADMIN_EMAIL, ADMIN_PASSWORD);
        bootstrap.run(null);
        bootstrap.run(null);

        verify(userRepository, times(1)).save(org.mockito.ArgumentMatchers.any(User.class));
        assertThat(userCount.get()).isEqualTo(1L);
    }

    private ApplicationRunner bootstrap(String email, String password) {
        return securityConfiguration.bootstrapAdmin(userRepository, passwordEncoder, email, password);
    }
}
