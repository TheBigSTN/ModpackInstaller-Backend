package com.stelian.modpack_service;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

//@RestControllerAdvice
//public class GlobalExceptionHandler {
//
//    @ExceptionHandler(ResponseStatusException.class)
//    public ResponseEntity<ErrorResponse> handleStatusException(ResponseStatusException ex) {
//        ErrorResponse error = new ErrorResponse(
//                LocalDateTime.now(),
//                ex.getStatusCode().value(),
//                ex.getReason()
//        );
//        return new ResponseEntity<>(error, ex.getStatusCode());
//    }
//
//    // Prinde și erorile neprevăzute (500 Internal Server Error)
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
//        ErrorResponse error = new ErrorResponse(
//                LocalDateTime.now(),
//                500,
//                "A apărut o eroare internă: " + ex.getMessage()
//        );
//        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
//    }
//
//    // DTO-ul pentru eroare (îl poți pune și ca record separat)
//    @Data
//    @AllArgsConstructor
//    public static class ErrorResponse {
//        private LocalDateTime timestamp;
//        private int status;
//        private String message;
//    }
//}