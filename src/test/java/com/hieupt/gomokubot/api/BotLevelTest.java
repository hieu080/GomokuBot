package com.hieupt.gomokubot.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BotLevel Tests")
class BotLevelTest {

    @ParameterizedTest
    @EnumSource(BotLevel.class)
    @DisplayName("Các level đều có thông số cấu hình hợp lệ")
    void testLevelConfigurations(BotLevel level) {
        assertThat(level.getDisplayName()).isNotBlank();
        assertThat(level.getMaxDepth()).isGreaterThan(0);
        assertThat(level.getDefaultTimeMs()).isGreaterThanOrEqualTo(100L);
    }

    @Test
    @DisplayName("Level EASY có depth nhỏ và tắt VCF/VCT")
    void testEasyLevel() {
        BotLevel easy = BotLevel.EASY;
        assertThat(easy.getMaxDepth()).isEqualTo(2);
        assertThat(easy.isVcfEnabled()).isFalse();
        assertThat(easy.isVctEnabled()).isFalse();
        assertThat(easy.isUseOpeningBook()).isFalse();
    }

    @Test
    @DisplayName("Level MASTER có depth cao nhất và bật toàn bộ tính năng")
    void testMasterLevel() {
        BotLevel master = BotLevel.MASTER;
        assertThat(master.getMaxDepth()).isEqualTo(12);
        assertThat(master.isVcfEnabled()).isTrue();
        assertThat(master.isVctEnabled()).isTrue();
        assertThat(master.isUseOpeningBook()).isTrue();
    }

    @Test
    @DisplayName("fromString phân tích chuỗi hợp lệ và fallback sang HARD khi null/không hợp lệ")
    void testFromString() {
        assertThat(BotLevel.fromString("EASY")).isEqualTo(BotLevel.EASY);
        assertThat(BotLevel.fromString("medium")).isEqualTo(BotLevel.MEDIUM);
        assertThat(BotLevel.fromString("hard")).isEqualTo(BotLevel.HARD);
        assertThat(BotLevel.fromString("MASTER")).isEqualTo(BotLevel.MASTER);
        assertThat(BotLevel.fromString(null)).isEqualTo(BotLevel.HARD);
        assertThat(BotLevel.fromString("UNKNOWN")).isEqualTo(BotLevel.HARD);
    }
}

