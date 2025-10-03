package com.reliaquest.api.controller;

import com.reliaquest.api.model.Employee;
import com.reliaquest.api.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EmployeeController.class)
class EmployeeControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private EmployeeService employeeService;

    @Test
    void getAllEmployees_returnsOk() throws Exception {
        when(employeeService.getAll()).thenReturn(List.of(Employee.builder().id("1").name("A").build()));
        mockMvc.perform(get("/api/v2/employee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is("1")));
    }

    @Test
    void getEmployeeById_notFound() throws Exception {
        when(employeeService.getById("x")).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v2/employee/x"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createEmployee_badRequestOnEmptyBody() throws Exception {
        mockMvc.perform(post("/api/v2/employee").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}


