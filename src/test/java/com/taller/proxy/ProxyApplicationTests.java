package com.taller.proxy;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "payment.failure-rate=0")
@AutoConfigureMockMvc
class ProxyApplicationTests {

    @Autowired
    MockMvc mvc;

    @Test
    void callIsLoggedThroughProxy() throws Exception {
        mvc.perform(post("/api/services/inventory/get").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"SKU-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"));
        mvc.perform(get("/api/metrics/logs?service=inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.content[0].requestId").isNotEmpty());
    }

    @Test
    void errorsAreLoggedWithStackTrace() throws Exception {
        mvc.perform(post("/api/services/inventory/get").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"NOPE\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/metrics/logs?status=ERROR"))
                .andExpect(jsonPath("$.content[0].error").value(org.hamcrest.Matchers.containsString("NotFoundException")));
    }

    @Test
    void simulateLoadGeneratesFiftyCalls() throws Exception {
        mvc.perform(post("/api/metrics/simulate-load")).andExpect(jsonPath("$.generated").value(50));
        mvc.perform(get("/api/metrics/summary")).andExpect(jsonPath("$.length()").value(3));
    }
}
