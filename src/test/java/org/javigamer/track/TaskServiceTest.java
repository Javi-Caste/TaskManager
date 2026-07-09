package org.javigamer.track;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.javigamer.task.Task;
import org.javigamer.task.TaskForm;
import org.javigamer.task.TaskNotFoundException;
import org.javigamer.task.TaskRepository;
import org.javigamer.task.TaskService;
import org.javigamer.user.CurrentUser;
import org.javigamer.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-04T18:30:00Z"),
            ZoneId.of("UTC"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 4, 18, 30);
    private static final CurrentUser JAVI = new CurrentUser("Javi", Role.USER);
    private static final CurrentUser ALEX = new CurrentUser("Alex", Role.USER);
    private static final CurrentUser ADMIN = new CurrentUser("admin", Role.ADMIN);

    @Mock
    private TaskRepository taskRepository;

    private TaskService taskService;

    @BeforeEach
    void setUpService() {
        taskService = new TaskService(taskRepository, FIXED_CLOCK);
    }

    @Test
    void createTaskNormalizesOwnerTrimsInputAndAssignsStartedAt() {
        TaskForm form = new TaskForm("  Build UI  ", "  Thymeleaf screens  ");
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(10L);
            return task;
        });

        Task savedTask = taskService.createTask(JAVI, form);

        assertThat(savedTask.getId()).isEqualTo(10L);
        assertThat(savedTask.getOwner()).isEqualTo("Javi");
        assertThat(savedTask.getName()).isEqualTo("Build UI");
        assertThat(savedTask.getDescription()).isEqualTo("Thymeleaf screens");
        assertThat(savedTask.getStartedAt()).isEqualTo(NOW);
        assertThat(savedTask.getFinishedAt()).isNull();
    }

    @Test
    void createTaskStoresNullDescription() {
        TaskForm form = new TaskForm("Task", "   ");
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task savedTask = taskService.createTask(JAVI, form);

        assertThat(savedTask.getOwner()).isEqualTo("Javi");
        assertThat(savedTask.getDescription()).isNull();
        assertThat(savedTask.getStartedAt()).isEqualTo(NOW);
        assertThat(savedTask.getFinishedAt()).isNull();
    }

    @Test
    void adminCannotCreateTasks() {
        TaskForm form = new TaskForm("Task", null);

        assertThatThrownBy(() -> taskService.createTask(ADMIN, form))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("ADMIN");

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void getTaskReturnsTaskOwnedByRequesterEvenWhenFinished() {
        Task task = task(1L, "Javi", "Read");
        task.setFinishedAt(NOW.plusHours(1));
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        Task foundTask = taskService.getTask(1L, JAVI);

        assertThat(foundTask).isSameAs(task);
    }

    @Test
    void getTaskThrowsWhenTaskDoesNotBelongToOwner() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task(1L, "Alex", "Read")));

        assertThatThrownBy(() -> taskService.getTask(1L, JAVI))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getTaskReturnsNotFoundWhenTaskDoesNotExist() {
        when(taskRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTask(1L, JAVI))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    void adminCanGetTaskOwnedByAnyone() {
        Task task = task(1L, "Alex", "Read");
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        Task foundTask = taskService.getTask(1L, ADMIN);

        assertThat(foundTask).isSameAs(task);
    }

    @Test
    void getAllTasksReturnsOnlyActiveTasksForOwner() {
        List<Task> tasks = List.of(task(1L, "Javi", "First"), task(2L, "Javi", "Second"));
        when(taskRepository.findAllByOwnerAndFinishedAtIsNullOrderByStartedAtAscIdAsc("Javi")).thenReturn(tasks);

        List<Task> result = taskService.getAllTasks(JAVI);

        assertThat(result).containsExactlyElementsOf(tasks);
    }

    @Test
    void adminGetsAllActiveTasks() {
        List<Task> tasks = List.of(task(1L, "Javi", "First"), task(2L, "Alex", "Second"));
        when(taskRepository.findAllByFinishedAtIsNullOrderByStartedAtAscIdAsc()).thenReturn(tasks);

        List<Task> result = taskService.getAllTasks(ADMIN);

        assertThat(result).containsExactlyElementsOf(tasks);
    }

    @Test
    void updateTaskPreservesOwnerAndLifecycleDates() {
        Task existingTask = task(1L, "Javi", "Old name");
        TaskForm form = new TaskForm(" New name ", " New description ");
        when(taskRepository.findByIdAndFinishedAtIsNull(1L)).thenReturn(Optional.of(existingTask));
        when(taskRepository.save(existingTask)).thenReturn(existingTask);

        Task updatedTask = taskService.updateTask(1L, JAVI, form);

        assertThat(updatedTask.getOwner()).isEqualTo("Javi");
        assertThat(updatedTask.getName()).isEqualTo("New name");
        assertThat(updatedTask.getDescription()).isEqualTo("New description");
        assertThat(updatedTask.getStartedAt()).isEqualTo(LocalDateTime.of(2026, 7, 4, 12, 0));
        assertThat(updatedTask.getFinishedAt()).isNull();
    }

    @Test
    void updateTaskDoesNotSaveWhenTaskIsMissingOrFinished() {
        TaskForm form = new TaskForm("Name", null);
        when(taskRepository.findByIdAndFinishedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.updateTask(99L, JAVI, form))
                .isInstanceOf(TaskNotFoundException.class);

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void userCannotUpdateAnotherUsersTask() {
        Task existingTask = task(1L, "Alex", "Old name");
        TaskForm form = new TaskForm("New name", null);
        when(taskRepository.findByIdAndFinishedAtIsNull(1L)).thenReturn(Optional.of(existingTask));

        assertThatThrownBy(() -> taskService.updateTask(1L, JAVI, form))
                .isInstanceOf(AccessDeniedException.class);

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void adminCanUpdateAnotherUsersTask() {
        Task existingTask = task(1L, "Alex", "Old name");
        TaskForm form = new TaskForm(" New name ", " New description ");
        when(taskRepository.findByIdAndFinishedAtIsNull(1L)).thenReturn(Optional.of(existingTask));
        when(taskRepository.save(existingTask)).thenReturn(existingTask);

        Task updatedTask = taskService.updateTask(1L, ADMIN, form);

        assertThat(updatedTask.getOwner()).isEqualTo("Alex");
        assertThat(updatedTask.getName()).isEqualTo("New name");
        assertThat(updatedTask.getDescription()).isEqualTo("New description");
    }

    @Test
    void deleteTaskSoftDeletesOwnedTaskByAssigningFinishedAt() {
        Task task = task(1L, "Javi", "Finish me");
        when(taskRepository.findByIdAndFinishedAtIsNull(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        taskService.deleteTask(1L, JAVI);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());
        verify(taskRepository, never()).delete(any(Task.class));
        assertThat(taskCaptor.getValue()).isSameAs(task);
        assertThat(taskCaptor.getValue().getFinishedAt()).isEqualTo(NOW);
    }

    @Test
    void deleteTaskThrowsWhenTaskIsMissingOrAlreadyFinished() {
        when(taskRepository.findByIdAndFinishedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.deleteTask(1L, JAVI))
                .isInstanceOf(TaskNotFoundException.class);

        verify(taskRepository, never()).save(any(Task.class));
        verify(taskRepository, never()).delete(any(Task.class));
    }

    @Test
    void userCannotDeleteAnotherUsersTask() {
        when(taskRepository.findByIdAndFinishedAtIsNull(1L)).thenReturn(Optional.of(task(1L, "Alex", "Other")));

        assertThatThrownBy(() -> taskService.deleteTask(1L, JAVI))
                .isInstanceOf(AccessDeniedException.class);

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void adminCannotDeleteTasks() {
        assertThatThrownBy(() -> taskService.deleteTask(1L, ADMIN))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("ADMIN");

        verify(taskRepository, never()).findByIdAndFinishedAtIsNull(1L);
        verify(taskRepository, never()).save(any(Task.class));
    }

    private Task task(Long id, String owner, String name) {
        return new Task(
                id,
                owner,
                name,
                "Description",
                LocalDateTime.of(2026, 7, 4, 12, 0),
                null);
    }
}
