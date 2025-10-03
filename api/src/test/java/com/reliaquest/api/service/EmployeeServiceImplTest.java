package com.reliaquest.api.service;

import com.reliaquest.api.exception.TooManyRequestsException;
import com.reliaquest.api.model.CreateEmployeeInput;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.ServerEmployee;
import com.reliaquest.api.model.ServerResponse;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class EmployeeServiceImplTest {

    @Mock private RestTemplate restTemplate;

    private EmployeeService service;

    private static final String BASE_URL = "http://localhost:8112/api/v1/employee";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new EmployeeServiceImpl(restTemplate, BASE_URL);
    }

    private ServerEmployee createServerEmployee(String id, String name, int salary, int age, String title, String email) {
        ServerEmployee emp = new ServerEmployee();
        emp.setId(id);
        emp.setEmployeeName(name);
        emp.setEmployeeSalary(salary);
        emp.setEmployeeAge(age);
        emp.setEmployeeTitle(title);
        emp.setEmployeeEmail(email);
        return emp;
    }

    private ServerResponse<List<ServerEmployee>> createServerResponse(List<ServerEmployee> employees) {
        ServerResponse<List<ServerEmployee>> response = new ServerResponse<>();
        response.setData(employees);
        return response;
    }

    private ServerResponse<ServerEmployee> createServerResponse(ServerEmployee employees) {
        ServerResponse<ServerEmployee> response = new ServerResponse<>();
        response.setData(employees);
        return response;
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

    @Test
    void searchByName_filtersByFragment() {
        when(restTemplate.exchange(eq(BASE_URL), eq(HttpMethod.GET), eq(null), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(createServerResponse(List.of(
                        createServerEmployee("1", "Alice", 100, 30, "Dev", "a@x.com"),
                        createServerEmployee("2", "Bob", 200, 35, "Dev", "b@x.com")
                ))));
        final var result = service.searchByName("ali");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Alice");
    }

    @Test
    void getHighestSalary_returnsMaxSalary() {
        when(restTemplate.exchange(eq(BASE_URL), eq(HttpMethod.GET), eq(null), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(createServerResponse(List.of(
                        createServerEmployee("1", "Alice", 100, 30, "Dev", "a@x.com"),
                        createServerEmployee("2", "Bob", 200, 35, "Dev", "b@x.com")
                ))));

        final int highest = service.getHighestSalary();
        assertThat(highest).isEqualTo(200);
    }

    @Test
    void getTopTenNamesBySalary_returnsTopNames() {
        when(restTemplate.exchange(eq(BASE_URL), eq(HttpMethod.GET), eq(null), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(createServerResponse(List.of(
                        createServerEmployee("1", "Alice", 100, 30, "Dev", "a@x.com"),
                        createServerEmployee("2", "Bob", 200, 35, "Dev", "b@x.com"),
                        createServerEmployee("3", "Charlie", 150, 32, "Dev", "c@x.com")
                ))));

        final var names = service.getTopTenNamesBySalary();
        assertThat(names).containsExactly("Bob", "Charlie", "Alice"); // sorted by salary descending
    }

    @Test
    void deleteById_returnsOptionalNameOnSuccess() {
        when(restTemplate.exchange(eq(BASE_URL + "/1"), eq(HttpMethod.GET), eq(null), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(createServerResponse(createServerEmployee("1", "Alice", 100, 30, "Dev", "a@x.com"))));

        ServerResponse<Boolean> deleteResponse = new ServerResponse<>();
        deleteResponse.setData(true);
        when(restTemplate.exchange(eq(BASE_URL), eq(HttpMethod.DELETE), any(), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(deleteResponse));

        final var result = service.deleteById("1");
        assertThat(result).isPresent().contains("Alice");
    }

    @Test
    void deleteById_returnsEmptyWhenNotFound() {
        when(restTemplate.exchange(eq(BASE_URL + "/1"), eq(HttpMethod.GET), eq(null), any(ParameterizedTypeReference.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatusCode.valueOf(404), "Not Found", null, null, null));

        final var result = service.deleteById("1");
        assertThat(result).isEmpty();
    }

    @Test
    void create_returnsEmployeeOnSuccess() {
        CreateEmployeeInput input = new CreateEmployeeInput();
        input.setName("Alice");
        input.setTitle("Dev");
        input.setSalary(100);
        input.setAge(30);

        final var serverEmployee = createServerEmployee("1", "Alice", 100, 30, "Dev", "a@x.com");
        final var payload = new ServerResponse<ServerEmployee>();
        payload.setData(serverEmployee);

        when(restTemplate.exchange(eq(BASE_URL), eq(HttpMethod.POST), any(), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(payload));

        final var result = service.create(input);
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Alice");
    }


}


