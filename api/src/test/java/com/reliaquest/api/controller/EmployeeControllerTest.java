package com.reliaquest.api.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.reliaquest.api.controller.EmployeeController;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.service.EmployeeService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = EmployeeController.class)
class EmployeeControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private EmployeeService employeeService;

    @Test
    void getAllEmployees_returnsOk() throws Exception {
        when(employeeService.getAll()).thenReturn(List.of(Employee.builder().id("1").name("A").build()));
        mockMvc.perform(get("/api/v1/employee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is("1")));
    }

    @Test
    void getEmployeeById_notFound() throws Exception {
        when(employeeService.getById("x")).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/employee/x"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createEmployee_badRequestOnEmptyBody() throws Exception {
        mockMvc.perform(post("/api/v1/employee").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}


