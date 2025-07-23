package com.xm.cryptoservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class LoadedFile {

    @Id
    private String filename;

    private Instant loadedAt;

    private long lastModified; // file last modified time in epoch millis

    public LoadedFile() {
    }

    public LoadedFile(String filename, Instant loadedAt, long lastModified) {
        this.filename = filename;
        this.loadedAt = loadedAt;
        this.lastModified = lastModified;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Instant getLoadedAt() {
        return loadedAt;
    }

    public void setLoadedAt(Instant loadedAt) {
        this.loadedAt = loadedAt;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
}
