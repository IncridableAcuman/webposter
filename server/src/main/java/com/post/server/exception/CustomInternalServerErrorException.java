package com.post.server.exception;

public class CustomInternalServerErrorException extends RuntimeException{
    public CustomInternalServerErrorException(){
        super("Internal Server Error");
    }
}
