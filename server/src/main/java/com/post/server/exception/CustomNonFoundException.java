package com.post.server.exception;

public class CustomNonFoundException extends RuntimeException{
    public CustomNonFoundException(String message){
        super(message);
    }
}
