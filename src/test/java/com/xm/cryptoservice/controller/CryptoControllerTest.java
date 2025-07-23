package com.xm.cryptoservice.controller;

import com.xm.cryptoservice.dto.CryptoStatsResponse;
import com.xm.cryptoservice.dto.NormalizedRangeResponse;
import com.xm.cryptoservice.exception.DataNotFoundException;
import com.xm.cryptoservice.exception.UnsupportedCryptoException;
import com.xm.cryptoservice.service.CryptoService;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CryptoController.class)
public class CryptoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CryptoService cryptoService;

    @Test
    void getStats_success() throws Exception {
        CryptoStatsResponse response = new CryptoStatsResponse(
                "BTC",
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(200),
                BigDecimal.valueOf(90),
                BigDecimal.valueOf(210)
        );
        when(cryptoService.getStats(eq("BTC"), any(), any())).thenReturn(response);

        mockMvc.perform(get("/cryptos/BTC/stats")
                        .param("from", "2023-01-01")
                        .param("to", "2023-12-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.symbol").value("BTC"))
                .andExpect(jsonPath("$.oldest").value(100))
                .andExpect(jsonPath("$.newest").value(200))
                .andExpect(jsonPath("$.min").value(90))
                .andExpect(jsonPath("$.max").value(210));
    }

    @Test
    void getStats_unsupportedSymbol_returns400() throws Exception {
        when(cryptoService.getStats(eq("DOGE"), any(), any()))
                .thenThrow(new UnsupportedCryptoException("DOGE")); // or whatever ctor you use

        mockMvc.perform(get("/cryptos/DOGE/stats"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Unsupported crypto: DOGE"));
    }


    @Test
    void getStats_noData_throws() throws Exception {
        when(cryptoService.getStats("BTC",null,null))
                .thenThrow(new DataNotFoundException("No data available for BTC"));

        mockMvc.perform(get("/cryptos/BTC/stats"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("No data available for BTC"));
    }

    @Test
    void getNormalizedRanges_success() throws Exception {
        List<NormalizedRangeResponse> list = List.of(
                new NormalizedRangeResponse("BTC", BigDecimal.valueOf(0.5)),
                new NormalizedRangeResponse("ETH", BigDecimal.valueOf(0.3))
        );
        when(cryptoService.getNormalizedRangeSorted(any(), any())).thenReturn(list);
        mockMvc.perform(get("/cryptos/normalized-range/all")
                        .param("from", "2023-01-01")
                        .param("to", "2023-12-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].symbol").value("BTC"))
                .andExpect(jsonPath("$[0].normalizedRange").value(0.5))
                .andExpect(jsonPath("$[1].symbol").value("ETH"))
                .andExpect(jsonPath("$[1].normalizedRange").value(0.3));
    }

    @Test
    void getNormalizedRanges_emptyList() throws Exception {

        when(cryptoService.getNormalizedRangeSorted(any(), any())).thenReturn(List.of());
        mockMvc.perform(get("/cryptos/normalized-range/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getMaxNormalizedCrypto_success() throws Exception {
        NormalizedRangeResponse response =
                new NormalizedRangeResponse("BTC", BigDecimal.valueOf(0.7));
        when(cryptoService.getMaxNormalizedCryptoOnDate("2023-07-22"))
                .thenReturn(response);

        mockMvc.perform(get("/cryptos/normalized-range/max")
                        .param("date", "2023-07-22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("BTC"))
                .andExpect(jsonPath("$.normalizedRange").value(0.7));
    }

    @Test
    void getMaxNormalizedCrypto_missingDateParam() throws Exception {
        mockMvc.perform(get("/cryptos/normalized-range/max"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMaxNormalizedCrypto_invalidDateFormat() throws Exception {
        when(cryptoService.getMaxNormalizedCryptoOnDate("invalid-date"))
                .thenThrow(new RuntimeException("Invalid date format"));

        mockMvc.perform(get("/cryptos/normalized-range/max")
                        .param("date", "invalid-date"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getMaxNormalizedCrypto_noData() throws Exception {
        when(cryptoService.getMaxNormalizedCryptoOnDate("2023-07-22"))
                .thenThrow(new DataNotFoundException("No data available for 2023-07-22"));

        mockMvc.perform(get("/cryptos/normalized-range/max")
                        .param("date", "2023-07-22"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("No data available for 2023-07-22"));
    }
}
