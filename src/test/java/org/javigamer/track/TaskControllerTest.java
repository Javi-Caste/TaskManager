package org.javigamer.track;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.javigamer.task.Task;
import org.javigamer.task.TaskController;
import org.javigamer.task.TaskForm;
import org.javigamer.task.TaskNotFoundException;
import org.javigamer.task.TaskOwnerResolver;
import org.javigamer.task.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerTest {

    private static final String OWNER = "Javi";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private TaskOwnerResolver ownerResolver;

    @BeforeEach
    void setUpOwner() {
        when(ownerResolver.resolve(any(Authentication.class))).thenReturn(OWNER);
    }

    @Test
    void getTaskReturnsTaskById() throws Exception {
        Task task = task(1L, OWNER, "Build tests");
        when(taskService.getTask(1L, OWNER)).thenReturn(task);

        mockMvc.perform(get("/tasks/{id}", 1L).principal(authenticatedOwner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.owner").value("Javi"))
                .andExpect(jsonPath("$.name").value("Build tests"))
                .andExpect(jsonPath("$.description").value("Description"))
                .andExpect(jsonPath("$.startedAt").value("2026-07-04T12:00:00"));

        verify(taskService).getTask(1L, OWNER);
    }

    @Test
    void getTaskReturnsNotFoundForMissingTask() throws Exception {
        when(taskService.getTask(99L, OWNER)).thenThrow(new TaskNotFoundException(99L));

        mockMvc.perform(get("/tasks/{id}", 99L).principal(authenticatedOwner()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No se encontro la tarea con id 99"));
    }

    @Test
    void getAllTasksReturnsTasks() throws Exception {
        Task firstTask = task(1L, OWNER, "Build tests");
        Task secondTask = task(2L, OWNER, "Review tests");
        when(taskService.getAllTasks(OWNER)).thenReturn(List.of(firstTask, secondTask));

        mockMvc.perform(get("/tasks").principal(authenticatedOwner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Build tests"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Review tests"));

        verify(taskService).getAllTasks(OWNER);
    }

    @Test
    void createTaskReturnsCreatedTask() throws Exception {
        Task savedTask = task(1L, OWNER, "Create task");
        when(taskService.createTask(eq(OWNER), any(TaskForm.class))).thenReturn(savedTask);

        mockMvc.perform(post("/tasks")
                        .principal(authenticatedOwner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson("Create task", "Persist a new task")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.owner").value("Javi"))
                .andExpect(jsonPath("$.name").value("Create task"))
                .andExpect(jsonPath("$.startedAt").value("2026-07-04T12:00:00"));

        ArgumentCaptor<TaskForm> formCaptor = ArgumentCaptor.forClass(TaskForm.class);
        verify(taskService).createTask(eq(OWNER), formCaptor.capture());
        assertThat(formCaptor.getValue().getName()).isEqualTo("Create task");
        assertThat(formCaptor.getValue().getDescription()).isEqualTo("Persist a new task");
    }

    @Test
    void createTaskDoesNotRequireDateFieldsInRequest() throws Exception {
        Task savedTask = task(1L, OWNER, "Create task");
        when(taskService.createTask(eq(OWNER), any(TaskForm.class))).thenReturn(savedTask);

        mockMvc.perform(post("/tasks")
                        .principal(authenticatedOwner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson("Create task", "No manual dates")))
                .andExpect(status().isCreated());
    }

    @Test
    void updateTaskReturnsUpdatedTask() throws Exception {
        Task updatedTask = task(1L, OWNER, "Update task");
        when(taskService.updateTask(eq(1L), eq(OWNER), any(TaskForm.class))).thenReturn(updatedTask);

        mockMvc.perform(put("/tasks/{id}", 1L)
                        .principal(authenticatedOwner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson("Update task", "Send changed task data")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.owner").value("Javi"))
                .andExpect(jsonPath("$.name").value("Update task"));

        verify(taskService).updateTask(eq(1L), eq(OWNER), any(TaskForm.class));
    }

    @Test
    void deleteTaskReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/tasks/{id}", 1L).principal(authenticatedOwner()))
                .andExpect(status().isNoContent());

        verify(taskService).deleteTask(1L, OWNER);
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

    private String taskJson(String name, String description) {
        return """
                {
                  "name": "%s",
                  "description": "%s"
                }
                """.formatted(name, description);
    }

    private TestingAuthenticationToken authenticatedOwner() {
        return new TestingAuthenticationToken(OWNER, "password");
    }
}
