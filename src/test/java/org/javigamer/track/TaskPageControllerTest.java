package org.javigamer.track;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDateTime;
import java.util.List;

import org.javigamer.task.Task;
import org.javigamer.task.TaskForm;
import org.javigamer.task.TaskNotFoundException;
import org.javigamer.task.TaskOwnerResolver;
import org.javigamer.task.TaskPageController;
import org.javigamer.task.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskPageController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskPageControllerTest {

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
    void homeRedirectsToTaskList() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app/tasks"));
    }

    @Test
    void listTasksRendersTaskList() throws Exception {
        when(taskService.getAllTasks(OWNER)).thenReturn(List.of(task(1L, OWNER, "Build UI")));

        mockMvc.perform(get("/app/tasks").principal(authenticatedOwner()))
                .andExpect(status().isOk())
                .andExpect(view().name("tasks/list"))
                .andExpect(model().attributeExists("tasks"))
                .andExpect(content().string(containsString("Build UI")))
                .andExpect(content().string(containsString("data-task-card=\"Build UI Description\"")))
                .andExpect(content().string(containsString("class=\"add-task-tile\"")))
                .andExpect(content().string(containsString("href=\"/app/tasks/new\"")));
    }

    @Test
    void listTasksRendersAddTileWhenListIsEmpty() throws Exception {
        when(taskService.getAllTasks(OWNER)).thenReturn(List.of());

        mockMvc.perform(get("/app/tasks").principal(authenticatedOwner()))
                .andExpect(status().isOk())
                .andExpect(view().name("tasks/list"))
                .andExpect(content().string(containsString("No hay tareas todav")))
                .andExpect(content().string(containsString("class=\"add-task-tile\"")))
                .andExpect(content().string(containsString("href=\"/app/tasks/new\"")));
    }

    @Test
    void newTaskRendersEmptyForm() throws Exception {
        mockMvc.perform(get("/app/tasks/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("tasks/form"))
                .andExpect(model().attributeExists("taskForm"))
                .andExpect(content().string(containsString("Nueva tarea")));
    }

    @Test
    void createTaskRedirectsToDetailWhenValid() throws Exception {
        when(taskService.createTask(eq(OWNER), any(TaskForm.class))).thenReturn(task(5L, OWNER, "New task"));

        mockMvc.perform(post("/app/tasks")
                        .principal(authenticatedOwner())
                        .param("name", "New task")
                        .param("description", "From form"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app/tasks/5"));

        verify(taskService).createTask(eq(OWNER), any(TaskForm.class));
    }

    @Test
    void createTaskRendersFormWhenInvalid() throws Exception {
        mockMvc.perform(post("/app/tasks")
                        .principal(authenticatedOwner())
                        .param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("tasks/form"))
                .andExpect(model().attributeHasFieldErrors("taskForm", "name"));
    }

    @Test
    void taskDetailsRendersTask() throws Exception {
        when(taskService.getTask(3L, OWNER)).thenReturn(task(3L, OWNER, "Read details"));

        mockMvc.perform(get("/app/tasks/{id}", 3L).principal(authenticatedOwner()))
                .andExpect(status().isOk())
                .andExpect(view().name("tasks/detail"))
                .andExpect(model().attributeExists("task"))
                .andExpect(content().string(containsString("Read details")))
                .andExpect(content().string(containsString("2026-07-04 12:00")));
    }

    @Test
    void editTaskRendersExistingTaskForm() throws Exception {
        when(taskService.getTask(3L, OWNER)).thenReturn(task(3L, OWNER, "Edit details"));

        mockMvc.perform(get("/app/tasks/{id}/edit", 3L).principal(authenticatedOwner()))
                .andExpect(status().isOk())
                .andExpect(view().name("tasks/form"))
                .andExpect(model().attributeExists("task", "taskForm"))
                .andExpect(content().string(containsString("Editar tarea")));
    }

    @Test
    void updateTaskRedirectsToDetailWhenValid() throws Exception {
        when(taskService.updateTask(eq(3L), eq(OWNER), any(TaskForm.class))).thenReturn(task(3L, OWNER, "Updated"));

        mockMvc.perform(post("/app/tasks/{id}", 3L)
                        .principal(authenticatedOwner())
                        .param("name", "Updated")
                        .param("description", "Updated from form"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app/tasks/3"));

        verify(taskService).updateTask(eq(3L), eq(OWNER), any(TaskForm.class));
    }

    @Test
    void updateTaskRendersFormWhenInvalid() throws Exception {
        when(taskService.getTask(3L, OWNER)).thenReturn(task(3L, OWNER, "Existing"));

        mockMvc.perform(post("/app/tasks/{id}", 3L)
                        .principal(authenticatedOwner())
                        .param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("tasks/form"))
                .andExpect(model().attributeHasFieldErrors("taskForm", "name"));
    }

    @Test
    void deleteTaskRedirectsToList() throws Exception {
        mockMvc.perform(post("/app/tasks/{id}/delete", 3L).principal(authenticatedOwner()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app/tasks"));

        verify(taskService).deleteTask(3L, OWNER);
    }

    @Test
    void missingTaskRendersNotFoundView() throws Exception {
        when(taskService.getTask(88L, OWNER)).thenThrow(new TaskNotFoundException(88L));

        mockMvc.perform(get("/app/tasks/{id}", 88L).principal(authenticatedOwner()))
                .andExpect(status().isOk())
                .andExpect(view().name("tasks/not-found"))
                .andExpect(content().string(containsString("No se encontro la tarea con id 88")));
    }

    @Test
    void invalidCreateDoesNotCallService() throws Exception {
        mockMvc.perform(post("/app/tasks")
                        .principal(authenticatedOwner())
                        .param("name", ""))
                .andExpect(status().isOk());

        verifyNoInteractions(taskService);
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

    private TestingAuthenticationToken authenticatedOwner() {
        return new TestingAuthenticationToken(OWNER, "password");
    }
}
