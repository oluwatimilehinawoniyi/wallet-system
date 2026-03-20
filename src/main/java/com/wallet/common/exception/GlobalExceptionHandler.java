package com.wallet.common.exception;

import com.wallet.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleWalletNotFound(WalletNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, exception);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientBalance(InsufficientBalanceException exception) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, exception);
    }

    @ExceptionHandler({
            DuplicateTransactionException.class,
            TransactionAlreadyReversedException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleConflict(RuntimeException exception) {
        return build(HttpStatus.CONFLICT, exception);
    }

    @ExceptionHandler({
            InvalidAmountException.class,
            BadRequestException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(RuntimeException exception) {
        return build(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(ObjectOptimisticLockingFailureException exception) {
        return build(HttpStatus.CONFLICT, exception);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Validation failed");
        return build(HttpStatus.BAD_REQUEST, message, exception);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException exception) {
        return build(HttpStatus.UNAUTHORIZED, exception);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleFallback(Exception exception) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, exception);
    }

    private ResponseEntity<ApiResponse<Void>> build(HttpStatus status, Exception exception) {
        log.error("Request failed", exception);
        return ResponseEntity.status(status).body(ApiResponse.failure(exception.getMessage()));
    }

    private ResponseEntity<ApiResponse<Void>> build(HttpStatus status, String message, Exception exception) {
        log.error("Request failed", exception);
        return ResponseEntity.status(status).body(ApiResponse.failure(message));
    }
}

