package net.enjoy.springboot.ihuzohr.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordDto {
    @NotEmpty(message = "Password should not be empty")
    private String password;

    @NotEmpty(message = "Confirm Password should not be empty")
    private String confirmPassword;
}
