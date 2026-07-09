package org.javigamer.user;

public record UserSummary(Long id, String username, Role role) {

    static UserSummary from(UserAccount userAccount) {
        return new UserSummary(userAccount.getId(), userAccount.getUsername(), userAccount.getRole());
    }
}
