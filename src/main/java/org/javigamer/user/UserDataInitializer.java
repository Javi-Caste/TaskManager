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
        createUserIfMissing("admin", "admin123", Role.ADMIN);
        createUserIfMissing("javi", "password", Role.USER);
        createUserIfMissing("alex", "password", Role.USER);
    }

    private void createUserIfMissing(String username, String password, Role role) {
        if (!userAccountRepository.existsByUsername(username)) {
            userAccountRepository.save(new UserAccount(username, passwordEncoder.encode(password), role));
        }
    }
}
