package com.jpos.adm.core.extension;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // 400  Bad Request
    BACKEND_ERROR("backend_error", "backend error", HttpStatus.BAD_REQUEST),
    BAD_REQUEST("bad_request", "bad request", HttpStatus.BAD_REQUEST),
    FEATURE_NOT_SUPPORTED("feature_not_supported", "feature not supported", HttpStatus.BAD_REQUEST),
    // 401 Unauthorized
    AUTHENTICATION_FAILURE("authentication_failure", "authentication failed", HttpStatus.UNAUTHORIZED),
    // 403 Forbidden
    KEY_EXPIRED_ERROR("key_expired_error", "expired key", HttpStatus.FORBIDDEN),
    NOT_AUTHORIZED("not_authorized", "not authorized", HttpStatus.FORBIDDEN),
    // 404 Not Found
    NOT_FOUND("not_found", "the requested object was not found", HttpStatus.NOT_FOUND),
    SERIALIZER_NOT_FOUND("serializer_not_found", "your current API version does not support this action", HttpStatus.NOT_FOUND),
    SERVICE_NOT_FOUND("service_not_found", "you are using api version which does not support this operation", HttpStatus.NOT_FOUND),
    // 422 Unprocessable Entity
    FAILED_DELETION("failed_deletion", "this object could not be deleted", HttpStatus.UNPROCESSABLE_ENTITY),
    INVALID_FILTER("invalid_filter", "invalid filters", HttpStatus.UNPROCESSABLE_ENTITY),
    INVALID_PAGE("invalid_page", "invalid page", HttpStatus.UNPROCESSABLE_ENTITY),
    INVALID_PER_PAGE("invalid_per_page", "invalid per page", HttpStatus.UNPROCESSABLE_ENTITY),
    INVALID_SCOPE("invalid_scope", "invalid scope", HttpStatus.UNPROCESSABLE_ENTITY),
    // 429 Too Many Requests
    TOO_MANY_REQUESTS("too_many_requests", "you have sent too many requests in too little time", HttpStatus.TOO_MANY_REQUESTS),
    // 500 Internal Server Error
    INTERNAL_ERROR("internal_error", "request could not be completed due to an internal error", HttpStatus.INTERNAL_SERVER_ERROR),
    // 503 Service Unavailable
    SEARCH_UNAVAILABLE("search_unavailable", "search is temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE);

    public final HttpStatus httpStatus;
    public final String code;
    public final String message;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}

