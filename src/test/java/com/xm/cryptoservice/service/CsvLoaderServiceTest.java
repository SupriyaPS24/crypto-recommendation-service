package com.xm.cryptoservice.service;

import com.xm.cryptoservice.exception.CsvProcessingException;
import com.xm.cryptoservice.model.LoadedFile;
import com.xm.cryptoservice.model.CryptoPrice;
import com.xm.cryptoservice.repository.CryptoPriceRepository;
import com.xm.cryptoservice.repository.LoadedFileRepository;
import com.opencsv.CSVWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CsvLoaderServiceTest {

    // Helper method to set private field via reflection
    private static void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getSupportedSymbols_returnsSet() {
        CryptoPriceRepository cryptoRepo = Mockito.mock(CryptoPriceRepository.class);
        LoadedFileRepository loadedFileRepo = Mockito.mock(LoadedFileRepository.class);
        String config = "BTC, ETH, XRP";

        CsvLoaderService service = new CsvLoaderService(cryptoRepo, loadedFileRepo, config);

        Set<String> symbols = service.getSupportedSymbols();

        assertTrue(symbols.contains("BTC"));
        assertTrue(symbols.contains("ETH"));
        assertTrue(symbols.contains("XRP"));
    }

    @Test
    void init_loadsCsvFiles_whenFileNotLoaded(@TempDir Path tempDir) throws Exception {
        CryptoPriceRepository cryptoRepo = Mockito.mock(CryptoPriceRepository.class);
        LoadedFileRepository loadedFileRepo = Mockito.mock(LoadedFileRepository.class);

        String config = "BTC,ETH";
        CsvLoaderService service = new CsvLoaderService(cryptoRepo, loadedFileRepo, config);

        File csvFile = tempDir.resolve("prices_values.csv").toFile(); // must match _values.csv suffix

        try (CSVWriter writer = new CSVWriter(new FileWriter(csvFile))) {
            writer.writeNext(new String[]{"timestamp", "symbol", "price"});
            writer.writeNext(new String[]{
                    String.valueOf(Instant.now().toEpochMilli()),
                    "BTC",
                    "30000.50"
            });
            writer.writeNext(new String[]{
                    String.valueOf(Instant.now().toEpochMilli()),
                    "ETH",
                    "2000.75"
            });
        }

        setPrivateField(service, "csvPath", tempDir.toString());

        when(loadedFileRepo.findById(csvFile.getName())).thenReturn(Optional.empty());

        service.init();

        verify(cryptoRepo, times(2)).save(Mockito.any(CryptoPrice.class));
        verify(loadedFileRepo, times(1)).save(Mockito.any(LoadedFile.class));
    }

    @Test
    void init_skipsFile_whenFileUnchanged(@TempDir Path tempDir) throws Exception {
        CryptoPriceRepository cryptoRepo = Mockito.mock(CryptoPriceRepository.class);
        LoadedFileRepository loadedFileRepo = Mockito.mock(LoadedFileRepository.class);

        String config = "BTC";
        CsvLoaderService service = new CsvLoaderService(cryptoRepo, loadedFileRepo, config);

        File csvFile = tempDir.resolve("prices_values.csv").toFile();

        try (CSVWriter writer = new CSVWriter(new FileWriter(csvFile))) {
            writer.writeNext(new String[]{"timestamp", "symbol", "price"});
            writer.writeNext(new String[]{String.valueOf(Instant.now().toEpochMilli()), "BTC", "30000.50"});
        }

        setPrivateField(service, "csvPath", tempDir.toString());

        long lastModified = csvFile.lastModified();

        LoadedFile loadedFile = new LoadedFile();
        loadedFile.setFilename(csvFile.getName());
        loadedFile.setLastModified(lastModified);
        loadedFile.setLoadedAt(Instant.now());

        when(loadedFileRepo.findById(csvFile.getName())).thenReturn(Optional.of(loadedFile));

        service.init();

        verify(cryptoRepo, never()).save(Mockito.any());
        verify(loadedFileRepo, never()).save(Mockito.any());
    }

    @Test
    void loadCsv_throwsOnMalformedLine(@TempDir Path tempDir) throws Exception {
        CryptoPriceRepository cryptoRepo = Mockito.mock(CryptoPriceRepository.class);
        LoadedFileRepository loadedFileRepo = Mockito.mock(LoadedFileRepository.class);

        CsvLoaderService service = new CsvLoaderService(cryptoRepo, loadedFileRepo, "BTC");

        File csvFile = tempDir.resolve("malformed_values.csv").toFile();

        try (CSVWriter writer = new CSVWriter(new FileWriter(csvFile))) {
            writer.writeNext(new String[]{"timestamp", "symbol", "price"});
            writer.writeNext(new String[]{"123456789", "BTC"}); // Missing price column
        }

        setPrivateField(service, "csvPath", tempDir.toString());

        CsvProcessingException ex = assertThrows(CsvProcessingException.class, service::init);

        String message = ex.getMessage();
        Throwable cause = ex.getCause();

        boolean containsMalformed = message.contains("Malformed CSV") ||
                (cause != null && cause.getMessage().contains("Malformed CSV"));

        assertTrue(containsMalformed, "Expected exception message to contain 'Malformed CSV', but was: " + message +
                (cause != null ? ", cause: " + cause.getMessage() : ""));
    }

    @Test
    void loadCsv_throwsOnUnsupportedSymbol(@TempDir Path tempDir) throws Exception {
        CryptoPriceRepository cryptoRepo = Mockito.mock(CryptoPriceRepository.class);
        LoadedFileRepository loadedFileRepo = Mockito.mock(LoadedFileRepository.class);

        CsvLoaderService service = new CsvLoaderService(cryptoRepo, loadedFileRepo, "BTC");

        File csvFile = tempDir.resolve("unsupported_values.csv").toFile();

        try (CSVWriter writer = new CSVWriter(new FileWriter(csvFile))) {
            writer.writeNext(new String[]{"timestamp", "symbol", "price"});
            writer.writeNext(new String[]{String.valueOf(Instant.now().toEpochMilli()), "DOGE", "0.05"});
        }

        setPrivateField(service, "csvPath", tempDir.toString());

        CsvProcessingException ex = assertThrows(CsvProcessingException.class, service::init);

        String message = ex.getMessage();
        Throwable cause = ex.getCause();

        boolean containsUnsupported = message.contains("Unsupported symbol") ||
                (cause != null && cause.getMessage().contains("Unsupported symbol"));

        assertTrue(containsUnsupported, "Expected exception message to contain 'Unsupported symbol', but was: " + message +
                (cause != null ? ", cause: " + cause.getMessage() : ""));
    }

}
