package com.example.taskmanager;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TaskApiTest {
    @Autowired
    MockMvc mvc;

    @Test
    void createsUpdatesFiltersAndDeletesTask() throws Exception {
        var created = mvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"学习 Spring Boot\",\"description\":\"完成 REST API\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("学习 Spring Boot"))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn();
        Number idValue = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        long id = idValue.longValue();

        mvc.perform(get("/api/tasks?status=active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(put("/api/tasks/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"学习 JPA\",\"description\":\"保存任务\",\"completed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("学习 JPA"))
                .andExpect(jsonPath("$.completed").value(true));

        mvc.perform(get("/api/tasks?status=active"))
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/tasks?status=completed"))
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/tasks?status=all"))
                .andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(delete("/api/tasks/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/tasks?status=all")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void rejectsBlankTitleAndUnknownStatus() throws Exception {
        mvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"   \",\"description\":\"x\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/tasks?status=unknown"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundForMissingTask() throws Exception {
        mvc.perform(put("/api/tasks/999999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"不存在\",\"description\":\"\",\"completed\":false}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/tasks/999999")).andExpect(status().isNotFound());
    }
}
