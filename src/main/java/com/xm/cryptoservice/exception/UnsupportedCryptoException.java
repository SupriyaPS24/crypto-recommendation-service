package com.xm.cryptoservice.exception;

public class UnsupportedCryptoException extends RuntimeException {
    public UnsupportedCryptoException(String symbol) {
        super("Unsupported crypto: " + symbol);
    }
}

