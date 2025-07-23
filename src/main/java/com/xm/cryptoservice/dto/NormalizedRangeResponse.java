package com.xm.cryptoservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Normalized range value for a cryptocurrency")
public record NormalizedRangeResponse(

        @Schema(description = "Symbol name (e.g., BTC)")
        String symbol,

        @Schema(description = "Normalized range (max - min / min) for the given symbol")
        BigDecimal normalizedRange

) {}
