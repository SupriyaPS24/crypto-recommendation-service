package com.xm.cryptoservice.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Statistics for a specific crypto symbol")
public record CryptoStatsResponse(
        @Schema(description = "Symbol name (e.g., BTC)")
        String symbol,

        @Schema(description = "Oldest price in time range")
        BigDecimal oldest,

        @Schema(description = "Newest price in time range")
        BigDecimal newest,

        @Schema(description = "Minimum price in time range")
        BigDecimal min,

        @Schema(description = "Maximum price in time range")
        BigDecimal max
) {}
