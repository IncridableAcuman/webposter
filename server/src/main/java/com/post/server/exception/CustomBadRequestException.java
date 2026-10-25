package com.post.server.exception;

public class CustomBadRequestException extends RuntimeException{
    public CustomBadRequestException(String message){
        super(message);
    }
}
