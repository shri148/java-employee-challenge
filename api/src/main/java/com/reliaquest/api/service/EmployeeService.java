package com.reliaquest.api.service;

import com.reliaquest.api.model.CreateEmployeeInput;
import com.reliaquest.api.model.Employee;
import java.util.List;
import java.util.Optional;

public interface EmployeeService {

    List<Employee> getAll();

    List<Employee> searchByName(String fragment);

    Optional<Employee> getById(String id);

    int getHighestSalary();

    List<String> getTopTenNamesBySalary();

    Optional<String> deleteById(String id);

    Optional<Employee> create(CreateEmployeeInput input);
}


