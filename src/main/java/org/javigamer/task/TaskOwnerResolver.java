package org.javigamer.task;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class TaskOwnerResolver {

    static final String DEFAULT_OWNER = "guest";

    public String resolve(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return DEFAULT_OWNER;
        }

        String name = authentication.getName();
        if (name == null || name.isBlank() || "anonymousUser".equals(name)) {
            return DEFAULT_OWNER;
        }

        return name.trim();
    }
}
