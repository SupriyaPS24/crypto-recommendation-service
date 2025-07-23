package com.xm.cryptoservice.repository;

import com.xm.cryptoservice.model.CryptoPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface CryptoPriceRepository extends JpaRepository<CryptoPrice, Long> {

    List<CryptoPrice> findBySymbolIgnoreCase(String symbol);

    @Query("SELECT c FROM CryptoPrice c WHERE c.timestamp BETWEEN :start AND :end")
    List<CryptoPrice> findByTimestampBetween(Instant start, Instant end);

    List<CryptoPrice> findBySymbolIgnoreCaseAndTimestampBetween(String symbol, Instant start, Instant end);
}
