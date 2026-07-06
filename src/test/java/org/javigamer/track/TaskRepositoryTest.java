package org.javigamer.track;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.javigamer.task.Task;
import org.javigamer.task.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void findByIdAndOwnerReturnsOnlyMatchingOwner() {
        Task javiTask = taskRepository.save(task("Javi", "Javi task", LocalDateTime.of(2026, 7, 4, 12, 0)));
        Task alexTask = taskRepository.save(task("Alex", "Alex task", LocalDateTime.of(2026, 7, 4, 12, 0)));

        assertThat(taskRepository.findByIdAndOwner(javiTask.getId(), "Javi")).contains(javiTask);
        assertThat(taskRepository.findByIdAndOwner(javiTask.getId(), "Alex")).isEmpty();
        assertThat(taskRepository.findByIdAndOwner(alexTask.getId(), "Javi")).isEmpty();
    }

    @Test
    void findAllByOwnerReturnsOnlyActiveTasksOrderedByStartedAtThenId() {
        Task later = taskRepository.save(task("Javi", "Later", LocalDateTime.of(2026, 7, 8, 12, 0)));
        Task earlier = taskRepository.save(task("Javi", "Earlier", LocalDateTime.of(2026, 7, 4, 12, 0)));
        Task otherOwner = taskRepository.save(task("Alex", "Other", LocalDateTime.of(2026, 7, 1, 12, 0)));
        Task finished = task("Javi", "Finished", LocalDateTime.of(2026, 7, 2, 12, 0));
        finished.setFinishedAt(LocalDateTime.of(2026, 7, 3, 12, 0));
        taskRepository.save(finished);

        assertThat(taskRepository.findAllByOwnerAndFinishedAtIsNullOrderByStartedAtAscIdAsc("Javi"))
                .containsExactly(earlier, later)
                .doesNotContain(otherOwner, finished);
    }

    @Test
    void findActiveByIdAndOwnerIgnoresFinishedTasks() {
        Task active = taskRepository.save(task("Javi", "Active", LocalDateTime.of(2026, 7, 4, 12, 0)));
        Task finished = task("Javi", "Finished", LocalDateTime.of(2026, 7, 4, 13, 0));
        finished.setFinishedAt(LocalDateTime.of(2026, 7, 4, 14, 0));
        finished = taskRepository.save(finished);

        assertThat(taskRepository.findByIdAndOwnerAndFinishedAtIsNull(active.getId(), "Javi")).contains(active);
        assertThat(taskRepository.findByIdAndOwnerAndFinishedAtIsNull(finished.getId(), "Javi")).isEmpty();
    }

    private Task task(String owner, String name, LocalDateTime startedAt) {
        return new Task(null, owner, name, "Description", startedAt, null);
    }
}
