package il.ac.jmc.matchday.init;

import il.ac.jmc.matchday.config.Roles;
import il.ac.jmc.matchday.entity.User;
import il.ac.jmc.matchday.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DataInitializerTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);

    private DataInitializer initializer(String password) {
        return new DataInitializer(users, encoder, "local-admin", "admin@example.test", password);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "short", "            "})
    void rejectsMissingWeakOrBlankPasswords(String password) {
        assertThatThrownBy(() -> initializer(password).run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("ADMIN_PASSWORD");
        verify(users, never()).save(any());
        verifyNoInteractions(encoder);
    }

    @Test
    void rejectsPasswordsBeyondBcryptByteLimit() {
        assertThatThrownBy(() -> initializer("x".repeat(73)).run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class);
        verify(users, never()).save(any());
    }

    @Test
    void neverReplacesAnExistingAdministrator() {
        when(users.existsByUsername("local-admin")).thenReturn(true);
        initializer("").run(new DefaultApplicationArguments());
        verify(users, never()).save(any());
        verifyNoInteractions(encoder);
    }

    @Test
    void createsAdministratorUsingEncodedPasswordAndExpectedRoles() {
        String password = "test-only-bootstrap-password";
        when(encoder.encode(password)).thenReturn("encoded-password");
        initializer(password).run(new DefaultApplicationArguments());
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("encoded-password");
        assertThat(saved.getValue().getUsername()).isEqualTo("local-admin");
        assertThat(saved.getValue().getRoles()).containsExactlyInAnyOrder(Roles.ADMIN, Roles.USER);
    }
}
