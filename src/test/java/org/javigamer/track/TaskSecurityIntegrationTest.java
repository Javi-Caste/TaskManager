package org.javigamer.track;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.javigamer.task.Task;
import org.javigamer.task.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void cleanTasks() {
        taskRepository.deleteAll();
    }

    @Test
    void unauthenticatedWebRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/app/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCreatesOwnTaskAndCannotOverrideOwnerFromRequest() throws Exception {
        mockMvc.perform(post("/tasks")
                        .with(httpBasic("javi", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "owner": "alex",
                                  "name": "Own task",
                                  "description": "Created through API"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.owner").value("javi"))
                .andExpect(jsonPath("$.name").value("Own task"));

        assertThat(taskRepository.findAll())
                .singleElement()
                .extracting(Task::getOwner)
                .isEqualTo("javi");
    }

    @Test
    void userSeesOnlyOwnTasks() throws Exception {
        taskRepository.save(task("javi", "Javi task"));
        taskRepository.save(task("alex", "Alex task"));

        mockMvc.perform(get("/tasks").with(httpBasic("javi", "password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].owner").value("javi"))
                .andExpect(jsonPath("$[0].name").value("Javi task"));
    }

    @Test
    void userCannotViewEditOrDeleteAnotherUsersTask() throws Exception {
        Task alexTask = taskRepository.save(task("alex", "Alex task"));

        mockMvc.perform(get("/tasks/{id}", alexTask.getId()).with(httpBasic("javi", "password")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/tasks/{id}", alexTask.getId())
                        .with(httpBasic("javi", "password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson("Changed", "Nope")))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/tasks/{id}", alexTask.getId()).with(httpBasic("javi", "password")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanViewAndEditAnyUsersTask() throws Exception {
        Task alexTask = taskRepository.save(task("alex", "Alex task"));

        mockMvc.perform(get("/tasks/{id}", alexTask.getId()).with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value("alex"))
                .andExpect(jsonPath("$.name").value("Alex task"));

        mockMvc.perform(put("/tasks/{id}", alexTask.getId())
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson("Admin edit", "Updated by admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value("alex"))
                .andExpect(jsonPath("$.name").value("Admin edit"));
    }

    @Test
    void adminCannotCreateTasksFromApiOrWeb() throws Exception {
        mockMvc.perform(post("/tasks")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson("Forbidden", "Admin cannot create")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/app/tasks/new").with(httpBasic("admin", "admin123")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/app/tasks")
                        .with(httpBasic("admin", "admin123"))
                        .with(csrf())
                        .param("name", "Forbidden")
                        .param("description", "Admin cannot create"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminGuiDoesNotShowCreateTaskActions() throws Exception {
        taskRepository.save(task("javi", "Javi task"));

        mockMvc.perform(get("/app/tasks").with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Buscar usuarios")))
                .andExpect(content().string(containsString("Buscar tarea por ID")))
                .andExpect(content().string(not(containsString("href=\"/app/tasks/new\""))))
                .andExpect(content().string(not(containsString("class=\"add-task-tile\""))));
    }

    @Test
    void adminCanSearchUsersAndUserCannot() throws Exception {
        mockMvc.perform(get("/users").param("query", "ja").with(httpBasic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("javi"))
                .andExpect(jsonPath("$[0].role").value("USER"));

        mockMvc.perform(get("/users").param("query", "ja").with(httpBasic("javi", "password")))
                .andExpect(status().isForbidden());
    }

    private Task task(String owner, String name) {
        return new Task(null, owner, name, "Description", LocalDateTime.of(2026, 7, 4, 12, 0), null);
    }

    private String taskJson(String name, String description) {
        return """
                {
                  "name": "%s",
                  "description": "%s"
                }
                """.formatted(name, description);
    }
}
