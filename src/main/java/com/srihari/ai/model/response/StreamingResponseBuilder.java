package com.srihari.ai.model.response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;

/**
 * Builder for streaming API responses.
 * Handles responses that contain streaming data like token streams.
 */
public class StreamingResponseBuilder<T> implements ResponseBuilder<T> {
    
    private String correlationId;
    private Instant timestamp = Instant.now();
    private String sessionId;
    private ApiResponseStatus status = ApiResponseStatus.PROCESSING;
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
    
    /**
     * Add streaming-specific metadata
     */
    public StreamingResponseBuilder<T> withStreamMetadata(String streamType, boolean isComplete) {
        this.metadata.put("streamType", streamType);
        this.metadata.put("isComplete", isComplete);
        this.metadata.put("streaming", true);
        return this;
    }
    
    /**
     * Set the stream as completed
     */
    public StreamingResponseBuilder<T> asCompleted() {
        this.status = ApiResponseStatus.SUCCESS;
        this.metadata.put("isComplete", true);
        return this;
    }
    
    @Override
    public ApiResponse<T> build() {
        // Add default streaming metadata if not present
        if (!metadata.containsKey("streaming")) {
            metadata.put("streaming", true);
        }
        
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
    
    /**
     * Build a streaming response wrapper for Flux data
     */
    public Flux<ApiResponse<T>> buildStream(Flux<T> dataStream) {
        return dataStream
                .map(item -> {
                    StreamingResponseBuilder<T> builder = new StreamingResponseBuilder<>();
                    builder.correlationId = this.correlationId;
                    builder.sessionId = this.sessionId;
                    builder.metadata = new HashMap<>(this.metadata);
                    builder.metadata.put("isComplete", false);
                    
                    return builder
                            .withData(item)
                            .withStatus(ApiResponseStatus.PROCESSING)
                            .build();
                })
                .concatWith(Flux.just(
                    ((StreamingResponseBuilder<T>) new StreamingResponseBuilder<T>()
                            .withCorrelationId(correlationId)
                            .withSessionId(sessionId)
                            .withMetadata(metadata))
                            .asCompleted()
                            .build()
                ));
    }
}