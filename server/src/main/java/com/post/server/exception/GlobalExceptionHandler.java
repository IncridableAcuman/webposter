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
    @ExceptionHandler(CustomNonFoundException.class)
    public ResponseEntity<ErrorResponse> customNotFoundExceptionHandler(CustomNonFoundException exception){
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
}
