package com.jpos.adm.core.handler;

import com.jpos.adm.core.extension.ErrorCode;
import com.jpos.adm.core.exception.*;
import com.jpos.adm.core.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@ControllerAdvice
public class ApiExceptionHandler {

    private final static String ERROR = "error";

    @ExceptionHandler({Exception.class})
    public ResponseEntity<ErrorResponse> handleException(HttpServletRequest request, Exception e) {
        return prepareErrorResponse(request, ErrorCode.INTERNAL_ERROR, e);
    }

    @ExceptionHandler({NotAuthorizedException.class})
    public ResponseEntity<ErrorResponse> handleForbiddenException(HttpServletRequest request, Exception e) {
        return prepareErrorResponse(request, ErrorCode.NOT_AUTHORIZED, e);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, ExcelException.class})
    public ResponseEntity<ErrorResponse> handleBadRequestException(HttpServletRequest request, Exception e) {
        if (e instanceof MethodArgumentTypeMismatchException) {
            return prepareErrorResponse(request, ErrorCode.BAD_REQUEST, e);
        }

        if (e instanceof ExcelException) {
            return prepareErrorResponse(request, ErrorCode.BACKEND_ERROR, e);
        }

        return prepareErrorResponse(request, ErrorCode.INTERNAL_ERROR, e);
    }

    @ExceptionHandler({NotFoundException.class, HttpMediaTypeNotSupportedException.class, HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<ErrorResponse> handleNotFoundException(HttpServletRequest request, Exception e) {

        if (e instanceof NotFoundException) {
            return prepareErrorResponse(request, ErrorCode.NOT_FOUND, e);
        }

        if (e instanceof HttpMediaTypeNotSupportedException) {
            return prepareErrorResponse(request, ErrorCode.SERIALIZER_NOT_FOUND, e);
        }

        if (e instanceof HttpRequestMethodNotSupportedException) {
            return prepareErrorResponse(request, ErrorCode.SERVICE_NOT_FOUND, e);
        }
        return prepareErrorResponse(request, ErrorCode.INTERNAL_ERROR, e);
    }

    @ExceptionHandler({CantDeleteException.class, FilterException.class})
    public ResponseEntity<ErrorResponse> handleUnprocessableEntityException(HttpServletRequest request, Exception e) {
        if (e instanceof CantDeleteException) {
            return prepareErrorResponse(request, ErrorCode.FAILED_DELETION, e);
        }

        if (e instanceof FilterException) {
            return prepareErrorResponse(request, ErrorCode.INVALID_FILTER, e);
        }
        return prepareErrorResponse(request, ErrorCode.INTERNAL_ERROR, e);
    }


    private ResponseEntity<ErrorResponse> prepareErrorResponse(HttpServletRequest request, ErrorCode errorCode, Exception e) {

        log.error(ExceptionUtils.getStackTrace(e), e);

        ErrorResponse errorResponse = new ErrorResponse(
                ERROR,
                request.getRequestURI(),
                errorCode.code,
                errorCode.message
        );
        return new ResponseEntity<>(errorResponse, errorCode.httpStatus);
    }
}
