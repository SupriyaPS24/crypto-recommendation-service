package com.xm.cryptoservice.service;

import com.xm.cryptoservice.dto.CryptoStatsResponse;
import com.xm.cryptoservice.dto.NormalizedRangeResponse;
import com.xm.cryptoservice.exception.DataNotFoundException;
import com.xm.cryptoservice.exception.UnsupportedCryptoException;
import com.xm.cryptoservice.model.CryptoPrice;
import com.xm.cryptoservice.repository.CryptoPriceRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CryptoService {

    private final CryptoPriceRepository repo;
    private final CsvLoaderService csvLoaderService;

    public CryptoService(CryptoPriceRepository repo, CsvLoaderService csvLoaderService) {
        this.repo = repo;
        this.csvLoaderService = csvLoaderService;
    }

    public CryptoStatsResponse getStats(String symbol, String from, String to) {
        validateSymbol(symbol);
        Instant fromTime = from != null ? parseInstant(from) : Instant.MIN;
        Instant toTime = to != null ? parseInstant(to) : Instant.MAX;

        // Use IgnoreCase method, symbol uppercased for validation only
        List<CryptoPrice> prices = repo.findBySymbolIgnoreCaseAndTimestampBetween(symbol, fromTime, toTime);

        if (prices.isEmpty()) {
            throw new DataNotFoundException("No data available for " + symbol);
        }

        BigDecimal min = prices.stream()
                .map(CryptoPrice::getPrice)
                .min(Comparator.naturalOrder())
                .orElseThrow();

        BigDecimal max = prices.stream()
                .map(CryptoPrice::getPrice)
                .max(Comparator.naturalOrder())
                .orElseThrow();

        BigDecimal oldest = prices.stream()
                .min(Comparator.comparing(CryptoPrice::getTimestamp))
                .map(CryptoPrice::getPrice)
                .orElseThrow();

        BigDecimal newest = prices.stream()
                .max(Comparator.comparing(CryptoPrice::getTimestamp))
                .map(CryptoPrice::getPrice)
                .orElseThrow();

        return new CryptoStatsResponse(symbol.toUpperCase(), oldest, newest, min, max);
    }

    public List<NormalizedRangeResponse> getNormalizedRangeSorted(String from, String to) {
        Instant fromTime = from != null ? parseInstant(from) : Instant.MIN;
        Instant toTime = to != null ? parseInstant(to) : Instant.MAX;

        List<String> symbols = new ArrayList<>(csvLoaderService.getSupportedSymbols());

        return symbols.stream()
                .map(symbol -> {
                    List<CryptoPrice> prices = repo.findBySymbolIgnoreCaseAndTimestampBetween(symbol, fromTime, toTime);
                    if (prices.size() < 2) return null;

                    BigDecimal min = prices.stream().map(CryptoPrice::getPrice).min(Comparator.naturalOrder()).orElseThrow();
                    BigDecimal max = prices.stream().map(CryptoPrice::getPrice).max(Comparator.naturalOrder()).orElseThrow();

                    if (min.compareTo(BigDecimal.ZERO) == 0) return null;

                    BigDecimal normalized = max.subtract(min).divide(min, 6, RoundingMode.HALF_UP);
                    return new NormalizedRangeResponse(symbol, normalized);
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(NormalizedRangeResponse::normalizedRange).reversed())
                .collect(Collectors.toList());
    }

    public NormalizedRangeResponse getMaxNormalizedCryptoOnDate(String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        Instant start = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        return csvLoaderService.getSupportedSymbols().stream()
                .map(symbol -> {
                    List<CryptoPrice> prices = repo.findBySymbolIgnoreCaseAndTimestampBetween(symbol, start, end);
                    if (prices.size() < 2) return null;

                    BigDecimal min = prices.stream().map(CryptoPrice::getPrice).min(Comparator.naturalOrder()).orElseThrow();
                    BigDecimal max = prices.stream().map(CryptoPrice::getPrice).max(Comparator.naturalOrder()).orElseThrow();

                    if (min.compareTo(BigDecimal.ZERO) == 0) return null;

                    BigDecimal normalized = max.subtract(min).divide(min, 6, RoundingMode.HALF_UP);
                    return new NormalizedRangeResponse(symbol, normalized);
                })
                .filter(Objects::nonNull)
                .max(Comparator.comparing(NormalizedRangeResponse::normalizedRange))
                .orElseThrow(() -> new DataNotFoundException("No data available for " + dateStr));
    }

    private void validateSymbol(String symbol) {
        if (!csvLoaderService.getSupportedSymbols().contains(symbol.toUpperCase())) {
            throw new UnsupportedCryptoException(symbol);
        }
    }

    private Instant parseInstant(String str) {
        return Instant.parse(str + "T00:00:00Z");
    }
}
