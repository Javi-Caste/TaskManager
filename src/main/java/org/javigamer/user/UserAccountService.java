package org.javigamer.user;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class UserAccountService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public UserAccountService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        UserAccount userAccount = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No se encontro el usuario " + username));

        return User.withUsername(userAccount.getUsername())
                .password(userAccount.getPassword())
                .roles(userAccount.getRole().name())
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserSummary> searchUsers(CurrentUser currentUser, String query) {
        requireAdmin(currentUser);

        String normalizedQuery = StringUtils.hasText(query) ? query.strip() : "";
        return userAccountRepository.findByUsernameContainingIgnoreCaseOrderByUsernameAsc(normalizedQuery).stream()
                .map(UserSummary::from)
                .toList();
    }

    private void requireAdmin(CurrentUser currentUser) {
        if (!currentUser.isAdmin()) {
            throw new AccessDeniedException("Solo ADMIN puede buscar usuarios");
        }
    }
}
