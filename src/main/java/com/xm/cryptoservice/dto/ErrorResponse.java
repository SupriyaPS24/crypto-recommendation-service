package com.xm.cryptoservice.dto;

public class ErrorResponse {

        private int statusCode;
        private String status;
        private String message;

        public ErrorResponse(int statusCode, String status, String message) {
            this.statusCode = statusCode;
            this.status = status;
            this.message = message;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }


