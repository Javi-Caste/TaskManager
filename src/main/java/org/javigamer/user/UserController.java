package org.javigamer.user;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserAccountService userAccountService;
    private final CurrentUserResolver currentUserResolver;

    public UserController(UserAccountService userAccountService, CurrentUserResolver currentUserResolver) {
        this.userAccountService = userAccountService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping
    public List<UserSummary> searchUsers(
            @RequestParam(defaultValue = "") String query,
            Authentication authentication) {
        return userAccountService.searchUsers(currentUserResolver.resolve(authentication), query);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String, String>> handleForbidden(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", exception.getMessage()));
    }
}
