package com.post.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthDto {
    @Data
    public static class RegisterRequest {
        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }
    @Data
    public static class LoginRequest{
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }
    @Data
    public static class ForgotPasswordRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
    }
    @Data
    public static class ResetPasswordRequest {
        @NotBlank(message = "Token is required")
        private String token;

        @NotBlank(message = "Password is required")
        private String password;

        @NotBlank(message = "Confirm password is required")
        private String confirmPassword;
    }
    public record AuthResponse(String accessToken){
        public static AuthResponse from(String accessToken){
            return new AuthResponse(accessToken);
        }
    }
}
