package com.srihari.ai.model.response;

/**
 * Enumeration of possible response statuses.
 */
public enum ApiResponseStatus {
    SUCCESS,
    ERROR,
    PARTIAL,
    PROCESSING,
    TIMEOUT,
    RATE_LIMITED,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    INTERNAL_ERROR
}