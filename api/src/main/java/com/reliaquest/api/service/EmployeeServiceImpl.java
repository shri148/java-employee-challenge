package com.reliaquest.api.service;

import com.reliaquest.api.exception.TooManyRequestsException;
import com.reliaquest.api.model.CreateEmployeeInput;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.ServerEmployee;
import com.reliaquest.api.model.ServerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final RestTemplate restTemplate;
    private final String serverBaseUrl;

    @Override
    public List<Employee> getAll() {
        try {
            log.debug("Fetching all employees from server: {}", serverBaseUrl);
            final ResponseEntity<ServerResponse<List<ServerEmployee>>> response = restTemplate.exchange(
                    serverBaseUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    });
            final List<ServerEmployee> serverEmployees = Optional.ofNullable(response.getBody())
                    .map(ServerResponse::getData)
                    .orElseGet(ArrayList::new);
            final List<Employee> mapped = serverEmployees.stream().map(this::toEmployee).toList();
            log.debug("Fetched {} employees", mapped.size());
            return mapped;
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw buildTooManyRequests(e);
        }
    }

    @Override
    public List<Employee> searchByName(String fragment) {
        return getAll().stream()
                .filter(e -> Objects.nonNull(e.getName()) && e.getName().toLowerCase().contains(fragment.toLowerCase()))
                .toList();
    }

    @Override
    public Optional<Employee> getById(String id) {
        try {
            final String url = serverBaseUrl + "/" + id;
            log.debug("Fetching employee by id: {}", id);
            final ResponseEntity<ServerResponse<ServerEmployee>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    });
            final ServerEmployee serverEmployee = Optional.ofNullable(response.getBody()).map(ServerResponse::getData).orElse(null);
            final Optional<Employee> result = Optional.ofNullable(serverEmployee).map(this::toEmployee);
            log.debug("Employee by id {} found: {}", id, result.isPresent());
            return result;
        } catch (HttpClientErrorException.NotFound e) {
            log.info("Employee not found by id: {}", id);
            return Optional.empty();
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw buildTooManyRequests(e);
        }
    }

    @Override
    public int getHighestSalary() {
        log.debug("Computing highest salary from employees list");
        final int highest = getAll().stream()
                .map(Employee::getSalary)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(0);
        log.debug("Computed highest salary: {}", highest);
        return highest;
    }

    @Override
    public List<String> getTopTenNamesBySalary() {
        log.debug("Computing top ten highest earning employee names");
        final List<String> names = getAll().stream()
                .sorted(Comparator.comparing(Employee::getSalary, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .map(Employee::getName)
                .toList();
        log.debug("Top ten names: {}", names);
        return names;
    }

    @Override
    public Optional<String> deleteById(String id) {
        log.debug("Deleting employee by id: {}", id);
        final Optional<Employee> employeeOpt = getById(id);
        if (employeeOpt.isEmpty()) {
            log.info("Delete skipped; employee not found for id: {}", id);
            return Optional.empty();
        }
        final String name = employeeOpt.get().getName();
        final DeleteByName deleteInput = new DeleteByName(name);
        final HttpEntity<DeleteByName> entity = new HttpEntity<>(deleteInput);
        final boolean success;
        try {
            final ResponseEntity<ServerResponse<Boolean>> response = restTemplate.exchange(
                    serverBaseUrl,
                    HttpMethod.DELETE,
                    entity,
                    new ParameterizedTypeReference<>() {
                    });
            success = Optional.ofNullable(response.getBody()).map(ServerResponse::getData).orElse(false);
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw buildTooManyRequests(e);
        }
        if (Boolean.TRUE.equals(success)) {
            log.info("Deleted employee by id: {} with name: {}", id, name);
            return Optional.of(name);
        }
        log.info("Delete failed; employee not removed for id: {}", id);
        return Optional.empty();
    }

    @Override
    public Optional<Employee> create(CreateEmployeeInput input) {
        log.debug("Creating employee: name={}, title={} salary={} age={}", input.getName(), input.getTitle(), input.getSalary(), input.getAge());
        final HttpEntity<CreateEmployeeInput> entity = new HttpEntity<>(input);
        try {
            final ResponseEntity<ServerResponse<ServerEmployee>> response = restTemplate.exchange(
                    serverBaseUrl,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {
                    });
            final ServerEmployee serverEmployee = Optional.ofNullable(response.getBody()).map(ServerResponse::getData).orElse(null);
            final Optional<Employee> result = Optional.ofNullable(serverEmployee).map(this::toEmployee);
            log.info("Created employee: {}", result.map(Employee::getId).orElse("<none>"));
            return result;
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw buildTooManyRequests(e);
        }
    }

    private Employee toEmployee(ServerEmployee serverEmployee) {
        return Employee.builder()
                .id(serverEmployee.getId())
                .name(serverEmployee.getEmployeeName())
                .salary(serverEmployee.getEmployeeSalary())
                .age(serverEmployee.getEmployeeAge())
                .title(serverEmployee.getEmployeeTitle())
                .email(serverEmployee.getEmployeeEmail())
                .build();
    }

    private TooManyRequestsException buildTooManyRequests(HttpClientErrorException.TooManyRequests e) {
        Long retryAfterSeconds = null;
        if (e.getResponseHeaders() != null
                && e.getResponseHeaders().getFirst("Retry-After") != null) {
            try {
                retryAfterSeconds = Long.parseLong(e.getResponseHeaders().getFirst("Retry-After"));
            } catch (NumberFormatException ignored) {
                // Ignore invalid Retry-After header value
            }
        }
        String message = "Rate limit exceeded. Please retry after some time.";
        return new TooManyRequestsException(message, retryAfterSeconds);
    }

    private static class DeleteByName {
        private String name;

        public DeleteByName(String name) {
            this.name = name;
        }
    }
}


