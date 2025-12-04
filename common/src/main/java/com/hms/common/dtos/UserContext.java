package com.hms.common.dtos;

public class UserContext {

    private static final ThreadLocal<CurrentUser> CURRENT = new ThreadLocal<>();

    public static void set(String id, String email, String role) {
        CURRENT.set(new CurrentUser(id, email, role));
    }

    public static CurrentUser get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
