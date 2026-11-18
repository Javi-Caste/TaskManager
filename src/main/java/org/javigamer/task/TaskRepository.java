package org.javigamer.task;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Optional<Task> findByIdAndFinishedAtIsNull(Long id);

    List<Task> findAllByFinishedAtIsNullOrderByStartedAtAscIdAsc();

    List<Task> findAllByOwner_IdAndFinishedAtIsNullOrderByStartedAtAscIdAsc(Long ownerId);
}
