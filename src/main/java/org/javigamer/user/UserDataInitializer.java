package org.javigamer.user;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserDataInitializer implements CommandLineRunner {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserDataInitializer(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createUserIfMissing("admin", "admin@local.track", "admin123", Role.ADMIN);
        createUserIfMissing("javi", "javi@local.track", "password", Role.USER);
        createUserIfMissing("alex", "alex@local.track", "password", Role.USER);
    }

    private void createUserIfMissing(String username, String email, String password, Role role) {
        if (!userAccountRepository.existsByUsername(username)) {
            userAccountRepository.save(new UserAccount(username, email, passwordEncoder.encode(password), role));
        }
    }
}
