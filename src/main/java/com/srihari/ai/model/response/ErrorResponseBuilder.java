package com.srihari.ai.model.response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder for error API responses.
 */
public class ErrorResponseBuilder<T> implements ResponseBuilder<T> {
    
    private String correlationId;
    private Instant timestamp = Instant.now();
    private String sessionId;
    private ApiResponseStatus status = ApiResponseStatus.ERROR;
    private T data;
    private Map<String, Object> metadata = new HashMap<>();
    private List<ErrorInfo> errors = new ArrayList<>();
    
    @Override
    public ResponseBuilder<T> withCorrelationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }
    
    @Override
    public ResponseBuilder<T> withTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
        return this;
    }
    
    @Override
    public ResponseBuilder<T> withMetadata(Map<String, Object> metadata) {
        if (metadata != null) {
            this.metadata.putAll(metadata);
        }
        return this;
    }
    
    @Override
    public ResponseBuilder<T> withMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }
    
    @Override
    public ResponseBuilder<T> withSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }
    
    @Override
    public ResponseBuilder<T> withStatus(ApiResponseStatus status) {
        this.status = status;
        return this;
    }
    
    @Override
    public ResponseBuilder<T> withError(ErrorInfo error) {
        if (error != null) {
            this.errors.add(error);
        }
        return this;
    }
    
    @Override
    public ResponseBuilder<T> withError(String errorCode, String errorMessage) {
        this.errors.add(ErrorInfo.simple(errorCode, errorMessage));
        return this;
    }
    
    @Override
    public ResponseBuilder<T> withData(T data) {
        this.data = data;
        return this;
    }
    
    @Override
    public ApiResponse<T> build() {
        return ApiResponse.<T>builder()
                .correlationId(correlationId)
                .timestamp(timestamp)
                .sessionId(sessionId)
                .status(status)
                .data(data)
                .metadata(metadata.isEmpty() ? null : metadata)
                .errors(errors.isEmpty() ? null : errors)
                .build();
    }
}