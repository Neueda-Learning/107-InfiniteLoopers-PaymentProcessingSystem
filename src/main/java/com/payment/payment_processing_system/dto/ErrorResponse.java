package com.payment.payment_processing_system.dto;

import com.payment.payment_processing_system.enums.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Standardized DTO for error responses returned by the GlobalExceptionHandler.
 * Provides a consistent error structure across all API endpoints.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    /** The timestamp when the error occurred. */
    private LocalDateTime timestamp;

    /** The HTTP status code (e.g. 404, 400, 500). */
    private int status;

    /** The HTTP status reason phrase (e.g. "Not Found", "Bad Request"). */
    private String error;

    /** Machine-readable error code for programmatic handling by clients. */
    private ErrorCode errorCode;

    /** A human-readable description of what went wrong. */
    private String message;

    /** The request URI path that triggered the error. */
    private String path;
}
