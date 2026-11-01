package com.emin.roomsplit.group;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class JoinCodeGeneratorTest {

    private final JoinCodeGenerator generator = new JoinCodeGenerator();

    @Test
    void kodImaTacnoOsamZnakovaBezZnakovaKojiSeBrkaju() {
        for (int i = 0; i < 200; i++) {
            assertThat(generator.generate())
                    .hasSize(JoinCodeGenerator.LENGTH)
                    .matches("[ABCDEFGHJKMNPQRSTUVWXYZ2-9]+");
        }
    }

    @Test
    void kodoviSeNePonavljaju() {
        Set<String> kodovi = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            kodovi.add(generator.generate());
        }
        assertThat(kodovi).hasSize(1000);
    }
}
