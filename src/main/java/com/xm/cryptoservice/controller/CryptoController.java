package com.xm.cryptoservice.controller;

import com.xm.cryptoservice.dto.CryptoStatsResponse;
import com.xm.cryptoservice.dto.NormalizedRangeResponse;
import com.xm.cryptoservice.service.CryptoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/cryptos")
@Tag(name = "Crypto Stats", description = "Endpoints for crypto statistics and analysis")
public class CryptoController {

    private final CryptoService service;

    public CryptoController(CryptoService service) {
        this.service = service;
    }

    @GetMapping("/{symbol}/stats")
    @Operation(summary = "Get price statistics for a crypto symbol",
            description = "Returns oldest, newest, min, and max prices for the given symbol within optional date range.")
    public ResponseEntity<CryptoStatsResponse> getStats(
            @Parameter(description = "Crypto symbol, e.g., BTC", required = true)
            @PathVariable String symbol,

            @Parameter(description = "Start date (YYYY-MM-DD)", required = false,
                    schema = @Schema(type = "string", format = "date"))
            @RequestParam(required = false) String from,

            @Parameter(description = "End date (YYYY-MM-DD)", required = false,
                    schema = @Schema(type = "string", format = "date"))
            @RequestParam(required = false) String to) {

        return ResponseEntity.ok(service.getStats(symbol, from, to));
    }

    @GetMapping("/normalized-range/all")
    @Operation(summary = "Get normalized range for all supported cryptocurrencies",
            description = "Returns a list sorted by normalized range for each symbol in descending order.")
    public ResponseEntity<List<NormalizedRangeResponse>> getNormalizedRanges(
            @Parameter(description = "Start date (YYYY-MM-DD)", required = false,
                    schema = @Schema(type = "string", format = "date"))
            @RequestParam(required = false) String from,

            @Parameter(description = "End date (YYYY-MM-DD)", required = false,
                    schema = @Schema(type = "string", format = "date"))
            @RequestParam(required = false) String to) {

        return ResponseEntity.ok(service.getNormalizedRangeSorted(from, to));
    }

    @GetMapping("/normalized-range/max")
    @Operation(summary = "Get crypto with max normalized range on a specific date",
            description = "Returns the crypto with the highest normalized range on the given date.")
    public ResponseEntity<NormalizedRangeResponse> getMaxNormalizedCrypto(
            @Parameter(description = "Date (YYYY-MM-DD)", required = true,
                    schema = @Schema(type = "string", format = "date"))
            @RequestParam String date) {

        return ResponseEntity.ok(service.getMaxNormalizedCryptoOnDate(date));
    }
}
