package com.emin.roomsplit.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

import com.emin.roomsplit.config.JwtConfig;
import com.emin.roomsplit.user.User;

class TokenServiceTest {

    private static final String SECRET = "test-kljuc-samo-za-testove-nije-tajna-0123456789";

    private User korisnikSaId(long id) {
        User user = new User("ana@example.com", "HASH", "Ana");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void tokenSadrziIdKorisnikaIMozeSeProvjeriti() {
        JwtConfig config = new JwtConfig(SECRET);
        TokenService tokenService = new TokenService(config.jwtEncoder(), 60);

        String token = tokenService.generateToken(korisnikSaId(42L));
        Jwt jwt = config.jwtDecoder().decode(token);

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
        assertThat(tokenService.getValidity()).isEqualTo(Duration.ofMinutes(60));
    }

    @Test
    void tokenPotpisanDrugimKljucemSeOdbija() {
        TokenService tudjiServis = new TokenService(
                new JwtConfig("sasvim-drugi-kljuc-koji-je-dovoljno-dugacak-12345").jwtEncoder(), 60);
        JwtDecoder nasDecoder = new JwtConfig(SECRET).jwtDecoder();

        String laznoPotpisan = tudjiServis.generateToken(korisnikSaId(1L));

        assertThatThrownBy(() -> nasDecoder.decode(laznoPotpisan)).isInstanceOf(JwtException.class);
    }

    @Test
    void istekliTokenSeOdbija() {
        JwtConfig config = new JwtConfig(SECRET);
        // Token izdat prije 2 sata, a istekao prije 1 sat (iza tolerancije sata od 60 sekundi)
        Instant izdat = Instant.now().minus(Duration.ofHours(2));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("1")
                .issuedAt(izdat)
                .expiresAt(izdat.plus(Duration.ofHours(1)))
                .build();
        String token = config.jwtEncoder()
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        assertThatThrownBy(() -> config.jwtDecoder().decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void prekratakKljucSprjecavaStartAplikacije() {
        assertThatThrownBy(() -> new JwtConfig("kratak"))
                .isInstanceOf(IllegalStateException.class);
    }
}
