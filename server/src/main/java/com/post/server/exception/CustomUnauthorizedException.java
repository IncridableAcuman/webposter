package com.post.server.exception;

public class CustomUnauthorizedException extends RuntimeException{
    public CustomUnauthorizedException(String message){
        super(message);
    }
}
