package com.post.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
        @Size(min = 8,message = "Password must be at least 8 characters long")
        private String password;

        public RegisterRequest(String username, String email, String password) {
            this.username=username;
            this.email=email;
            this.password=password;
        }

    }
    @Data
    public static class LoginRequest{
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8,message = "Password must be at least 8 characters long")
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
        @Size(min = 8,message = "Password must be at least 8 characters long")
        private String password;

        @NotBlank(message = "Confirm password is required")
        @Size(min = 8,message = "Confirm password must be at least 8 characters long")
        private String confirmPassword;

        public boolean isPasswordMatching(){
            return this.password != null && this.password.equals(this.confirmPassword);
        }
    }
    public record AuthResponse(String accessToken){
        public static AuthResponse from(String accessToken){
            return new AuthResponse(accessToken);
        }
    }
}
