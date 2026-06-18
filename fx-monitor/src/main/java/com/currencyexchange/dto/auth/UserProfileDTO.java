package com.currencyexchange.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private Long id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String country;
    private String kycStatus;
    private Boolean isEmailVerified;
    private Boolean twoFactorEnabled;
    private Boolean isActive;
}
