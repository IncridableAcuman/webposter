package com.post.server.exception;

import com.post.server.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomUnauthorizedException.class)
    public ResponseEntity<ErrorResponse> customUnauthorizedExceptionHandler(CustomUnauthorizedException exception){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.from(exception,HttpStatus.UNAUTHORIZED));
    }
    @ExceptionHandler(CustomNotFoundException.class)
    public ResponseEntity<ErrorResponse> customNotFoundExceptionHandler(CustomNotFoundException exception){
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.from(exception,HttpStatus.NOT_FOUND));
    }
    @ExceptionHandler(CustomInternalServerErrorException.class)
    public ResponseEntity<ErrorResponse> customInternalServerErrorExceptionHandler(CustomInternalServerErrorException exception){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.from(exception,HttpStatus.INTERNAL_SERVER_ERROR));
    }
    @ExceptionHandler(CustomBadRequestException.class)
    public ResponseEntity<ErrorResponse> customBadRequestExceptionHandler(CustomBadRequestException exception){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.from(exception,HttpStatus.BAD_REQUEST));
    }
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(org.springframework.web.bind.MethodArgumentNotValidException exception){
        String message = "Validation error";
        if (exception.getBindingResult().getFieldError() != null) {
            message = exception.getBindingResult().getFieldError().getDefaultMessage();
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Bad Request", message, java.time.LocalDateTime.now()));
    }
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException exception){
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.from(exception, HttpStatus.NOT_FOUND));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception exception){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.from(exception,HttpStatus.INTERNAL_SERVER_ERROR));
    }
}
