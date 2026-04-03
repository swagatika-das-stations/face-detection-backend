package com.stations.facedetection.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.stations.facedetection.common.response.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

	 // 1. User Already Exists
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserExists(UserAlreadyExistsException ex) {

        ErrorResponse error = new ErrorResponse(
                ex.getMessage(),
                "USER_ALREADY_EXISTS"
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // 2. Password Mismatch
    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ErrorResponse> handlePasswordMismatch(PasswordMismatchException ex) {

        ErrorResponse error = new ErrorResponse(
                ex.getMessage(),
                "PASSWORD_MISMATCH"
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // 3. Resource Not Found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {

        ErrorResponse error = new ErrorResponse(
                ex.getMessage(),
                "RESOURCE_NOT_FOUND"
        );

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // 4. Generic Exception (Fallback)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {

        ErrorResponse error = new ErrorResponse(
                ex.getMessage(),
                "BAD_REQUEST"
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // 5. Generic Exception (Fallback)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {

        ErrorResponse error = new ErrorResponse(
                "Invalid email or password",
                "UNAUTHORIZED"
        );

        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    // 6. Generic authentication failures
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {

        ErrorResponse error = new ErrorResponse(
                "Authentication failed",
                "UNAUTHORIZED"
        );

        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    // 7. Generic Exception (Fallback)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {

        ErrorResponse error = new ErrorResponse(
                ex.getMessage(),
                "INTERNAL_SERVER_ERROR"
        );

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
