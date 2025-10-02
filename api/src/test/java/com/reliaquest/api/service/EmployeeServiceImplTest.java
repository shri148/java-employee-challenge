package com.reliaquest.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.reliaquest.api.exception.TooManyRequestsException;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.ServerEmployee;
import com.reliaquest.api.model.ServerResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

class EmployeeServiceImplTest {

    @Mock private RestTemplate restTemplate;

    private EmployeeService service;

    private static final String BASE_URL = "http://localhost:8112/api/v1/employee";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new EmployeeServiceImpl(restTemplate, BASE_URL);
    }

    @Test
    void getAll_mapsServerEmployees() {
        final var serverEmp = new ServerEmployee();
        serverEmp.setId("1");
        serverEmp.setEmployeeName("Alice");
        serverEmp.setEmployeeSalary(100);
        serverEmp.setEmployeeAge(30);
        serverEmp.setEmployeeTitle("Dev");
        serverEmp.setEmployeeEmail("a@x.com");
        final var payload = new ServerResponse<List<ServerEmployee>>();
        payload.setData(List.of(serverEmp));
        when(restTemplate.exchange(
                        eq(BASE_URL),
                        eq(HttpMethod.GET),
                        eq(null),
                        any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(payload));

        final var all = service.getAll();
        assertThat(all).hasSize(1);
        final Employee e = all.get(0);
        assertThat(e.getName()).isEqualTo("Alice");
        assertThat(e.getEmail()).isEqualTo("a@x.com");
    }

    @Test
    void getById_returnsEmptyOn404() {
        when(restTemplate.exchange(
                        eq(BASE_URL + "/1"),
                        eq(HttpMethod.GET),
                        eq(null),
                        any(ParameterizedTypeReference.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatusCode.valueOf(404), "Not Found", null, null, null));

        final Optional<Employee> result = service.getById("1");
        assertThat(result).isEmpty();
    }

    @Test
    void getAll_throwsTooManyRequests() {
        when(restTemplate.exchange(
                        eq(BASE_URL),
                        eq(HttpMethod.GET),
                        eq(null),
                        any(ParameterizedTypeReference.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatusCode.valueOf(429), "Too Many Requests", null, null, null));

        assertThatThrownBy(() -> service.getAll()).isInstanceOf(TooManyRequestsException.class);
    }
}


