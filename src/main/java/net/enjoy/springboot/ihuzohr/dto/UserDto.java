package net.enjoy.springboot.ihuzohr.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private Long id;
    @NotEmpty(message = "First name should not be empty")
    private String firstName;
    @NotEmpty(message = "Last name should not be empty")
    private String lastName;
    @NotEmpty(message = "Email should not be empty")
    @Email
    private String email;
//    @NotEmpty(message = "Password should not be empty")
    private String password;
    @NotEmpty(message = "Phone number should not be empty")
    private String phoneNumber;
    @Past(message = "Date of birth should be in the past")
    private LocalDate dateOfBirth;
}