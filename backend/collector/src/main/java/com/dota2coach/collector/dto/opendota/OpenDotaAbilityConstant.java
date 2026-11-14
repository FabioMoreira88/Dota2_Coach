package com.dota2coach.collector.dto.opendota;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Valor de GET /constants/abilities (mapa do nome interno da habilidade para os dados dela). {@code dmg_type}
 * ("Physical", "Magical", "Pure") às vezes vem como lista, por isso é lido como {@code Object}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenDotaAbilityConstant(@JsonProperty("dmg_type") Object damageType) {}
