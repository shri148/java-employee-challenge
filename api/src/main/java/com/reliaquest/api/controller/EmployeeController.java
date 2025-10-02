package com.reliaquest.api.controller;

import com.reliaquest.api.model.CreateEmployeeInput;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.service.EmployeeService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employee")
@RequiredArgsConstructor
@Slf4j
@Validated
public class EmployeeController implements IEmployeeController<Employee, CreateEmployeeInput> {

    private final EmployeeService employeeService;

    @Override
    public ResponseEntity<List<Employee>> getAllEmployees() {
        log.debug("HTTP GET /api/v1/employee - getAllEmployees");
        return ResponseEntity.ok(employeeService.getAll());
    }

    @Override
    public ResponseEntity<List<Employee>> getEmployeesByNameSearch(String searchString) {
        log.debug("HTTP GET /api/v1/employee/search/{}", searchString);
        return ResponseEntity.ok(employeeService.searchByName(searchString));
    }

    @Override
    public ResponseEntity<Employee> getEmployeeById(String id) {
        log.debug("HTTP GET /api/v1/employee/{}", id);
        return employeeService
                .getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @Override
    public ResponseEntity<Integer> getHighestSalaryOfEmployees() {
        log.debug("HTTP GET /api/v1/employee/highestSalary");
        return ResponseEntity.ok(employeeService.getHighestSalary());
    }

    @Override
    public ResponseEntity<List<String>> getTopTenHighestEarningEmployeeNames() {
        log.debug("HTTP GET /api/v1/employee/topTenHighestEarningEmployeeNames");
        return ResponseEntity.ok(employeeService.getTopTenNamesBySalary());
    }

    @Override
    public ResponseEntity<Employee> createEmployee(CreateEmployeeInput employeeInput) {
        log.debug("HTTP POST /api/v1/employee - createEmployee name={}", employeeInput.getName());
        return employeeService
                .create(employeeInput)
                .map(emp -> ResponseEntity.status(HttpStatus.OK).body(emp))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    }

    @Override
    public ResponseEntity<String> deleteEmployeeById(String id) {
        log.debug("HTTP DELETE /api/v1/employee/{}", id);
        return employeeService
                .deleteById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

}


