package org.javigamer.task;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Optional<Task> findByIdAndOwner(Long id, String owner);

    Optional<Task> findByIdAndOwnerAndFinishedAtIsNull(Long id, String owner);

    List<Task> findAllByOwnerAndFinishedAtIsNullOrderByStartedAtAscIdAsc(String owner);
}
