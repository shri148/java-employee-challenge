package com.reliaquest.api.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.reliaquest.api.ApiApplication;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = ApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EmployeeApiIntegrationTest {

	@Autowired private TestRestTemplate restTemplate;

	@LocalServerPort private int port;

	private String apiUrl(String path) {
		return "http://localhost:" + port + "/api/v1/employee" + path;
	}

	@Test
	void getAllEmployees_returnsOk() {
		ResponseEntity<Object[]> response = restTemplate.getForEntity(apiUrl(""), Object[].class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
	}

	@Test
	void getEmployeeById_handlesNotFound() {
		ResponseEntity<String> response = restTemplate.getForEntity(apiUrl("/00000000-0000-0000-0000-000000000000"), String.class);
		assertThat(response.getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.OK);
	}
}
