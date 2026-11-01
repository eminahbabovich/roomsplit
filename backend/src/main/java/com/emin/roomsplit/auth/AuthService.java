package com.emin.roomsplit.auth;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.emin.roomsplit.user.User;
import com.emin.roomsplit.user.UserRepository;
import com.emin.roomsplit.user.UserResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        // Isti email ne smije postojati u dva oblika (Ana@x.com i ana@x.com)
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        User user = new User(
                email,
                passwordEncoder.encode(request.password()),
                request.displayName().trim());

        try {
            return UserResponse.from(userRepository.save(user));
        } catch (DataIntegrityViolationException e) {
            // Dva istovremena zahtjeva su oba prošla existsByEmail; UNIQUE u bazi uhvati drugi
            throw new EmailAlreadyExistsException(email);
        }
    }
}
