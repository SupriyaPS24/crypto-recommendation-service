package com.xm.cryptoservice.service;

import com.opencsv.CSVReader;
import com.xm.cryptoservice.exception.CsvProcessingException;
import com.xm.cryptoservice.model.CryptoPrice;
import com.xm.cryptoservice.model.LoadedFile;
import com.xm.cryptoservice.repository.CryptoPriceRepository;
import com.xm.cryptoservice.repository.LoadedFileRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

@Component
public class CsvLoaderService {

    @Value("${data.csv-path}")
    private String csvPath;

    private final Set<String> supportedSymbols;
    private final CryptoPriceRepository cryptoPriceRepo;
    private final LoadedFileRepository loadedFileRepo;

    public CsvLoaderService(CryptoPriceRepository cryptoPriceRepo,
                            LoadedFileRepository loadedFileRepo,
                            @Value("${data.supported-symbols}") String supportedSymbolsConfig) {
        this.cryptoPriceRepo = cryptoPriceRepo;
        this.loadedFileRepo = loadedFileRepo;
        this.supportedSymbols = new HashSet<>();
        for (String sym : supportedSymbolsConfig.split(",")) {
            supportedSymbols.add(sym.trim().toUpperCase());
        }
    }

    public Set<String> getSupportedSymbols() {
        return supportedSymbols;
    }

    @PostConstruct
    public void init() {
        try (Stream<Path> paths = Files.walk(Paths.get(csvPath))) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith("_values.csv"))
                    .forEach(this::tryLoadCsv);
        } catch (IOException e) {
            throw new CsvProcessingException("Failed to access CSV directory: " + csvPath, e);
        }
    }

    private void tryLoadCsv(Path filePath) {
        String filename = filePath.getFileName().toString();
        long currentLastModified = filePath.toFile().lastModified();

        LoadedFile loadedFile = loadedFileRepo.findById(filename).orElse(null);

        if (loadedFile != null && loadedFile.getLastModified() == currentLastModified) {
            System.out.println("Skipping unchanged file: " + filename);
            return; // file unchanged, skip loading
        }

        // file is new or changed, load it
        loadCsv(filePath);

        if (loadedFile == null) {
            loadedFile = new LoadedFile();
            loadedFile.setFilename(filename);
        }
        loadedFile.setLastModified(currentLastModified);
        loadedFile.setLoadedAt(Instant.now());
        loadedFileRepo.save(loadedFile);
    }

    private void loadCsv(Path filePath) {
        int lineNumber = 0;

        try (CSVReader reader = new CSVReader(new FileReader(filePath.toFile()))) {
            String[] line;
            reader.readNext(); // Skip header
            lineNumber++;

            while ((line = reader.readNext()) != null) {
                lineNumber++;

                if (line.length < 3) {
                    throw new CsvProcessingException("Malformed CSV at line " + lineNumber + " in file " + filePath);
                }

                long timestamp;
                String symbol;
                BigDecimal price;

                try {
                    timestamp = Long.parseLong(line[0].trim());
                    symbol = line[1].trim().toUpperCase();
                    price = new BigDecimal(line[2].trim());
                } catch (NumberFormatException e) {
                    throw new CsvProcessingException("Invalid number format at line " + lineNumber + " in file " + filePath, e);
                }

                if (!supportedSymbols.contains(symbol)) {
                    throw new CsvProcessingException("Unsupported symbol '" + symbol + "' at line " + lineNumber + " in file " + filePath);
                }

                cryptoPriceRepo.save(new CryptoPrice(null, Instant.ofEpochMilli(timestamp), symbol, price));
            }

        } catch (IOException e) {
            throw new CsvProcessingException("I/O error while reading file: " + filePath, e);
        } catch (Exception e) {
            throw new CsvProcessingException("Unexpected error while processing file: " + filePath + " at line " + lineNumber, e);
        }
    }
}
