package com.currencyexchange.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDTO {

    private String email;
    private String password;
    private String fullName;
    private String phoneNumber;
    private String country;
}
