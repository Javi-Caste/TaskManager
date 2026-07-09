package org.javigamer.user;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserResolver {

    public CurrentUser resolve(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            throw new AccessDeniedException("Autenticacion requerida");
        }

        Role role = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))
                        ? Role.ADMIN
                        : Role.USER;

        return new CurrentUser(authentication.getName().strip(), role);
    }
}
