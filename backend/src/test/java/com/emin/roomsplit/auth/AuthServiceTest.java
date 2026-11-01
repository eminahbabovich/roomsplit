package com.emin.roomsplit.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.emin.roomsplit.user.User;
import com.emin.roomsplit.user.UserRepository;
import com.emin.roomsplit.user.UserResponse;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void prijavaSIspravnimPodacimaVracaToken() {
        User korisnik = new User("ana@example.com", "HASH", "Ana");
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(korisnik));
        when(passwordEncoder.matches("tajnaLozinka1", "HASH")).thenReturn(true);
        when(tokenService.generateToken(korisnik)).thenReturn("JWT");
        when(tokenService.getValidity()).thenReturn(Duration.ofMinutes(60));

        AuthResponse response = authService.login(new LoginRequest(" Ana@Example.com", "tajnaLozinka1"));

        assertThat(response.accessToken()).isEqualTo("JWT");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(3600);
        assertThat(response.user().email()).isEqualTo("ana@example.com");
    }

    @Test
    void prijavaSPogresnomLozinkomBacaIzuzetakBezTokena() {
        User korisnik = new User("ana@example.com", "HASH", "Ana");
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(korisnik));
        when(passwordEncoder.matches("pogresna", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ana@example.com", "pogresna")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(tokenService, never()).generateToken(any(User.class));
    }

    @Test
    void prijavaSNepostojecimEmailomDajeIstuGreskuKaoPogresnaLozinka() {
        when(userRepository.findByEmail("niko@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("niko@example.com", "bilo sta")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Pogrešan email ili lozinka");
    }

    @Test
    void registracijaSpremaHashLozinkeINormalizovaniEmail() {
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(passwordEncoder.encode("tajnaLozinka1")).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = authService.register(
                new RegisterRequest("  Ana@Example.com ", "tajnaLozinka1", " Ana "));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User spremljen = captor.getValue();

        assertThat(spremljen.getEmail()).isEqualTo("ana@example.com");
        assertThat(spremljen.getPasswordHash()).isEqualTo("HASH").isNotEqualTo("tajnaLozinka1");
        assertThat(spremljen.getDisplayName()).isEqualTo("Ana");
        assertThat(response.email()).isEqualTo("ana@example.com");
    }

    @Test
    void registracijaSaZauzetimEmailomBacaIzuzetakINeUpisuje() {
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("ana@example.com", "tajnaLozinka1", "Ana")))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void istovremenaRegistracijaKojuUhvatiBazaDajeIstiIzuzetak() {
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(passwordEncoder.encode("tajnaLozinka1")).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("unique"));

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("ana@example.com", "tajnaLozinka1", "Ana")))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }
}
