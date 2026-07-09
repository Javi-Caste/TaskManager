package org.javigamer.user;

public record CurrentUser(String username, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isUser() {
        return role == Role.USER;
    }
}
