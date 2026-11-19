package org.javigamer.track;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.javigamer.user.Role;
import org.javigamer.user.UserAccount;
import org.javigamer.user.UserAccountRepository;
import org.javigamer.user.UserAccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Test
    void disabledAccountIsReturnedAsDisabledToSpringSecurity() {
        UserAccount account = new UserAccount("javi", "javi@example.test", "encoded-password", Role.USER);
        account.setEnabled(false);
        when(userAccountRepository.findByUsername("javi")).thenReturn(Optional.of(account));

        UserDetails userDetails = new UserAccountService(userAccountRepository).loadUserByUsername("javi");

        assertThat(userDetails.isEnabled()).isFalse();
    }
}
