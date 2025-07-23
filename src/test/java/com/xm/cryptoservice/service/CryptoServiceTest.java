package com.xm.cryptoservice.service;

import com.xm.cryptoservice.dto.CryptoStatsResponse;
import com.xm.cryptoservice.dto.NormalizedRangeResponse;
import com.xm.cryptoservice.exception.DataNotFoundException;
import com.xm.cryptoservice.exception.UnsupportedCryptoException;
import com.xm.cryptoservice.model.CryptoPrice;
import com.xm.cryptoservice.repository.CryptoPriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CryptoServiceTest {

    @Mock
    CryptoPriceRepository repo;

    @Mock
    CsvLoaderService csvLoader;

    @InjectMocks
    CryptoService service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getStats_success_withFromAndTo() {
        String symbol = "BTC";
        List<CryptoPrice> prices = List.of(
                new CryptoPrice(1L, Instant.parse("2023-01-01T00:00:00Z"), symbol, new BigDecimal("100")),
                new CryptoPrice(2L, Instant.parse("2023-01-01T01:00:00Z"), symbol, new BigDecimal("150")),
                new CryptoPrice(3L, Instant.parse("2023-01-01T02:00:00Z"), symbol, new BigDecimal("120"))
        );

        when(csvLoader.getSupportedSymbols()).thenReturn(Set.of(symbol));
        when(repo.findBySymbolIgnoreCaseAndTimestampBetween(eq(symbol), any(), any())).thenReturn(prices);

        CryptoStatsResponse response = service.getStats(symbol, "2023-01-01", "2023-01-02");

        assertEquals(symbol, response.symbol());
        assertEquals(new BigDecimal("100"), response.oldest());
        assertEquals(new BigDecimal("120"), response.newest());
        assertEquals(new BigDecimal("100"), response.min());
        assertEquals(new BigDecimal("150"), response.max());
    }

    @Test
    void getStats_success_withNullFromTo_usesMinMaxInstant() {
        String symbol = "BTC";
        List<CryptoPrice> prices = List.of(
                new CryptoPrice(1L, Instant.parse("2023-01-01T00:00:00Z"), symbol, new BigDecimal("100")),
                new CryptoPrice(2L, Instant.parse("2023-01-01T01:00:00Z"), symbol, new BigDecimal("150"))
        );

        when(csvLoader.getSupportedSymbols()).thenReturn(Set.of(symbol));
        when(repo.findBySymbolIgnoreCaseAndTimestampBetween(eq(symbol), eq(Instant.MIN), eq(Instant.MAX)))
                .thenReturn(prices);

        CryptoStatsResponse response = service.getStats(symbol, null, null);

        assertEquals(symbol, response.symbol());
        assertEquals(new BigDecimal("100"), response.oldest());
        assertEquals(new BigDecimal("150"), response.newest());
    }

    @Test
    void getStats_unsupportedSymbol_throwsUnsupportedCryptoException() {
        when(csvLoader.getSupportedSymbols()).thenReturn(Set.of("ETH"));

        UnsupportedCryptoException ex = assertThrows(UnsupportedCryptoException.class,
                () -> service.getStats("BTC", null, null));

        assertTrue(ex.getMessage().contains("BTC"));
    }

    @Test
    void getStats_noData_throwsDataNotFoundException() {
        String symbol = "BTC";
        when(csvLoader.getSupportedSymbols()).thenReturn(Set.of(symbol));
        when(repo.findBySymbolIgnoreCaseAndTimestampBetween(eq(symbol), any(), any()))
                .thenReturn(Collections.emptyList());

        DataNotFoundException ex = assertThrows(DataNotFoundException.class,
                () -> service.getStats(symbol, null, null));

        assertTrue(ex.getMessage().contains(symbol));
    }

    @Test
    void getNormalizedRangeSorted_filtersOutSymbolsWithLessThanTwoPrices() {
        String sym1 = "BTC";
        String sym2 = "ETH";
        when(csvLoader.getSupportedSymbols()).thenReturn(Set.of(sym1, sym2));

        List<CryptoPrice> prices1 = List.of(
                new CryptoPrice(null, Instant.now(), sym1, new BigDecimal("100")),
                new CryptoPrice(null, Instant.now(), sym1, new BigDecimal("200"))
        );

        List<CryptoPrice> prices2 = Collections.singletonList(
                new CryptoPrice(null, Instant.now(), sym2, new BigDecimal("100"))
        );

        when(repo.findBySymbolIgnoreCaseAndTimestampBetween(eq(sym1), any(), any())).thenReturn(prices1);
        when(repo.findBySymbolIgnoreCaseAndTimestampBetween(eq(sym2), any(), any())).thenReturn(prices2);

        List<NormalizedRangeResponse> result = service.getNormalizedRangeSorted(null, null);

        assertEquals(1, result.size());
        assertEquals(sym1, result.get(0).symbol());
    }

    @Test
    void getNormalizedRangeSorted_filtersOutSymbolsWithZeroMinPrice() {
        String sym1 = "BTC";
        String sym2 = "ETH";
        when(csvLoader.getSupportedSymbols()).thenReturn(Set.of(sym1, sym2));

        List<CryptoPrice> prices1 = List.of(
                new CryptoPrice(null, Instant.now(), sym1, new BigDecimal("0")),
                new CryptoPrice(null, Instant.now(), sym1, new BigDecimal("0"))
        );

        List<CryptoPrice> prices2 = List.of(
                new CryptoPrice(null, Instant.now(), sym2, new BigDecimal("50")),
                new CryptoPrice(null, Instant.now(), sym2, new BigDecimal("100"))
        );

        when(repo.findBySymbolIgnoreCaseAndTimestampBetween(eq(sym1), any(), any())).thenReturn(prices1);
        when(repo.findBySymbolIgnoreCaseAndTimestampBetween(eq(sym2), any(), any())).thenReturn(prices2);

        List<NormalizedRangeResponse> result = service.getNormalizedRangeSorted(null, null);

        assertEquals(1, result.size());
        assertEquals(sym2, result.get(0).symbol());
    }

    @Test
    void getNormalizedRangeSorted_emptySupportedSymbols_returnsEmptyList() {
        when(csvLoader.getSupportedSymbols()).thenReturn(Collections.emptySet());

        List<NormalizedRangeResponse> result = service.getNormalizedRangeSorted(null, null);

        assertTrue(result.isEmpty());
    }

    @Test
    void getMaxNormalizedCryptoOnDate_success() {
        String dateStr = "2023-01-01";
        String symbol = "BTC";

        when(csvLoader.getSupportedSymbols()).thenReturn(Set.of(symbol));

        List<CryptoPrice> prices = List.of(
                new CryptoPrice(null, Instant.parse("2023-01-01T00:00:01Z"), symbol, new BigDecimal("100")),
                new CryptoPrice(null, Instant.parse("2023-01-01T23:59:59Z"), symbol, new BigDecimal("150"))
        );

        when(repo.findBySymbolIgnoreCaseAndTimestampBetween(eq(symbol), any(), any())).thenReturn(prices);

        NormalizedRangeResponse response = service.getMaxNormalizedCryptoOnDate(dateStr);

        assertEquals(symbol, response.symbol());
    }

    @Test
    void getMaxNormalizedCryptoOnDate_noData_throwsRuntimeException() {
        String dateStr = "2023-01-01";
        String symbol = "BTC";

        when(csvLoader.getSupportedSymbols()).thenReturn(Set.of(symbol));
        when(repo.findBySymbolIgnoreCaseAndTimestampBetween(eq(symbol), any(), any()))
                .thenReturn(Collections.emptyList());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.getMaxNormalizedCryptoOnDate(dateStr));

        assertTrue(ex.getMessage().contains(dateStr));
    }

    @Test
    void validateSymbol_throwsIfUnsupported() {
        when(csvLoader.getSupportedSymbols()).thenReturn(Set.of("ETH"));

        UnsupportedCryptoException ex = assertThrows(UnsupportedCryptoException.class,
                () -> service.getStats("btc", null, null));  // input lowercase, validation uppercases

        assertTrue(ex.getMessage().contains("btc"));
    }

    @Test
    void getStats_invalidFromDate_throwsDateTimeParseException() {
        when(csvLoader.getSupportedSymbols()).thenReturn(Set.of("BTC"));

        assertThrows(Exception.class, () -> service.getStats("BTC", "invalid-date", null));
    }
}
