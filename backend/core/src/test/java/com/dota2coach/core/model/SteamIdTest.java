package com.dota2coach.core.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SteamIdTest {

    @Test
    void converteSteamId64EmAccountIdENoSentidoContrario() {
        SteamId id = new SteamId(76_561_197_995_105_072L);

        assertThat(id.accountId()).isEqualTo(34_839_344L);
        assertThat(SteamId.fromAccountId(34_839_344L)).isEqualTo(id);
    }

    @Test
    void rejeitaValorForaDaFaixaDeContasIndividuais() {
        assertThatThrownBy(() -> new SteamId(12_345L)).isInstanceOf(IllegalArgumentException.class);
    }
}
