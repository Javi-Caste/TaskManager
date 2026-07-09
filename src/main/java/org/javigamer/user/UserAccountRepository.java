package org.javigamer.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsername(String username);

    List<UserAccount> findByUsernameContainingIgnoreCaseOrderByUsernameAsc(String username);
}
