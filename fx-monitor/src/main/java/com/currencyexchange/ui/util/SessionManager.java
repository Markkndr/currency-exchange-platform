package com.currencyexchange.ui.util;

import com.currencyexchange.dto.auth.AuthResponseDTO;

public class SessionManager {

    private static AuthResponseDTO session;

    public static void setSession(AuthResponseDTO auth) {
        session = auth;
    }

    public static AuthResponseDTO getSession() {
        return session;
    }

    public static void clearSession() {
        session = null;
    }

    public static boolean isLoggedIn() {
        return session != null;
    }
}
