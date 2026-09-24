package com.ucc.chatbot.util;

import java.util.Locale;

/**
 * Central role vocabulary for authorization decisions.
 *
 * <p>Role values are normalized before they are persisted or converted to
 * Spring Security authorities. This keeps legacy values such as USER and
 * SUPERADMIN from accidentally granting different permissions in different
 * controllers.</p>
 */
public final class RoleNames {

    public static final String VISITOR = "VISITOR";
    public static final String STUDENT = "STUDENT";
    public static final String ADMIN = "ADMIN";
    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String STAFF = "STAFF";
    public static final String EDITOR = "EDITOR";
    public static final String VIEWER = "VIEWER";

    private RoleNames() {
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return VISITOR;
        }
        String role = value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (role) {
            case "USER", "PUBLIC", "CUSTOMER" -> VISITOR;
            case "SUPERADMIN", "SUPER_ADMIN", "ROOT" -> SUPER_ADMIN;
            case "SUPER" -> SUPER_ADMIN;
            default -> role;
        };
    }

    public static boolean isAdmin(String value) {
        String role = normalize(value);
        return ADMIN.equals(role) || SUPER_ADMIN.equals(role);
    }

    public static boolean isStaff(String value) {
        String role = normalize(value);
        return isAdmin(role) || STAFF.equals(role) || EDITOR.equals(role) || VIEWER.equals(role);
    }

    public static String authority(String value) {
        return "ROLE_" + normalize(value);
    }
}