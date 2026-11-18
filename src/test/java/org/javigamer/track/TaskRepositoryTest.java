package org.javigamer.track;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.javigamer.task.Task;
import org.javigamer.task.TaskRepository;
import org.javigamer.user.Role;
import org.javigamer.user.UserAccount;
import org.javigamer.user.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Test
    void tasksReferenceTheirPersistedOwner() {
        UserAccount javi = user("javi");
        UserAccount alex = user("alex");
        Task javiTask = taskRepository.save(task(javi, "Javi task", LocalDateTime.of(2026, 7, 4, 12, 0)));
        Task alexTask = taskRepository.save(task(alex, "Alex task", LocalDateTime.of(2026, 7, 4, 12, 0)));

        assertThat(javiTask.getOwner().getId()).isEqualTo(javi.getId());
        assertThat(javiTask.getOwner().getUsername()).isEqualTo("javi");
        assertThat(alexTask.getOwner().getId()).isEqualTo(alex.getId());
    }

    @Test
    void findAllByOwnerReturnsOnlyActiveTasksOrderedByStartedAtThenId() {
        UserAccount javi = user("javi");
        UserAccount alex = user("alex");
        Task later = taskRepository.save(task(javi, "Later", LocalDateTime.of(2026, 7, 8, 12, 0)));
        Task earlier = taskRepository.save(task(javi, "Earlier", LocalDateTime.of(2026, 7, 4, 12, 0)));
        Task otherOwner = taskRepository.save(task(alex, "Other", LocalDateTime.of(2026, 7, 1, 12, 0)));
        Task finished = task(javi, "Finished", LocalDateTime.of(2026, 7, 2, 12, 0));
        finished.setFinishedAt(LocalDateTime.of(2026, 7, 3, 12, 0));
        taskRepository.save(finished);

        assertThat(taskRepository.findAllByOwner_IdAndFinishedAtIsNullOrderByStartedAtAscIdAsc(javi.getId()))
                .containsExactly(earlier, later)
                .doesNotContain(otherOwner, finished);
    }

    @Test
    void findActiveByIdIgnoresFinishedTasks() {
        UserAccount javi = user("javi");
        Task active = taskRepository.save(task(javi, "Active", LocalDateTime.of(2026, 7, 4, 12, 0)));
        Task finished = task(javi, "Finished", LocalDateTime.of(2026, 7, 4, 13, 0));
        finished.setFinishedAt(LocalDateTime.of(2026, 7, 4, 14, 0));
        finished = taskRepository.save(finished);

        assertThat(taskRepository.findByIdAndFinishedAtIsNull(active.getId())).contains(active);
        assertThat(taskRepository.findByIdAndFinishedAtIsNull(finished.getId())).isEmpty();
    }

    private UserAccount user(String username) {
        return userAccountRepository.save(new UserAccount(username, username + "@example.test", "encoded", Role.USER));
    }

    private Task task(UserAccount owner, String name, LocalDateTime startedAt) {
        return new Task(null, owner, name, "Description", startedAt, null);
    }
}
