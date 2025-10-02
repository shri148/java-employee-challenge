package com.reliaquest.api.it;

import com.reliaquest.api.ApiApplication;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = EmployeeApiWithMockServerIT.MockServerInitializer.class)
class EmployeeApiWithMockServerIT {

    static MockWebServer mockWebServer;

    @Autowired private TestRestTemplate restTemplate;

    @LocalServerPort private int port;

    static class MockServerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            try {
                mockWebServer = new MockWebServer();
                mockWebServer.start();
                String baseUrl = mockWebServer.url("/api/v1/employee").toString();
                TestPropertyValues.of("server.base-url=" + baseUrl).applyTo(applicationContext);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @AfterAll
    static void teardown() throws IOException {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    private String apiUrl(String path) {
        return "http://localhost:" + port + "/api/v1/employee" + path;
    }

    @Test
    void getAllEmployees_viaApi_usesMockServer() {
        String body = "{\n" +
                "  \"data\": [ {\n" +
                "    \"id\": \"1\",\n" +
                "    \"employee_name\": \"Alice\",\n" +
                "    \"employee_salary\": 100,\n" +
                "    \"employee_age\": 30,\n" +
                "    \"employee_title\": \"Dev\",\n" +
                "    \"employee_email\": \"a@x.com\"\n" +
                "  } ],\n" +
                "  \"status\": \"ok\"\n" +
                "}";
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(body).addHeader("Content-Type", "application/json"));

        var response = restTemplate.getForEntity(apiUrl(""), Object[].class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().length).isEqualTo(1);
    }

    @Test
    void getEmployeeById_viaApi_success() {
        String body = "{\n" +
                "  \"data\": {\n" +
                "    \"id\": \"2\",\n" +
                "    \"employee_name\": \"Bob\",\n" +
                "    \"employee_salary\": 200,\n" +
                "    \"employee_age\": 40,\n" +
                "    \"employee_title\": \"Tester\",\n" +
                "    \"employee_email\": \"b@x.com\"\n" +
                "  },\n" +
                "  \"status\": \"ok\"\n" +
                "}";
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(body).addHeader("Content-Type", "application/json"));

        var response = restTemplate.getForEntity(apiUrl("/2"), String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("Bob");
    }

    @Test
    void searchEmployees_viaApi_filtersByName() {
        String list = "{\n" +
                "  \"data\": [ {\n" +
                "    \"id\": \"1\",\n" +
                "    \"employee_name\": \"Alpha\",\n" +
                "    \"employee_salary\": 100,\n" +
                "    \"employee_age\": 30,\n" +
                "    \"employee_title\": \"Dev\",\n" +
                "    \"employee_email\": \"a@x.com\"\n" +
                "  }, {\n" +
                "    \"id\": \"2\",\n" +
                "    \"employee_name\": \"Beta\",\n" +
                "    \"employee_salary\": 150,\n" +
                "    \"employee_age\": 31,\n" +
                "    \"employee_title\": \"QA\",\n" +
                "    \"employee_email\": \"b@x.com\"\n" +
                "  } ],\n" +
                "  \"status\": \"ok\"\n" +
                "}";
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(list).addHeader("Content-Type", "application/json"));

        var response = restTemplate.getForEntity(apiUrl("/search/al"), Object[].class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().length).isEqualTo(1);
    }

    @Test
    void highestSalary_viaApi_computesFromList() {
        String list = "{\n" +
                "  \"data\": [ {\n" +
                "    \"id\": \"1\",\n" +
                "    \"employee_name\": \"A\",\n" +
                "    \"employee_salary\": 100,\n" +
                "    \"employee_age\": 30,\n" +
                "    \"employee_title\": \"Dev\",\n" +
                "    \"employee_email\": \"a@x.com\"\n" +
                "  }, {\n" +
                "    \"id\": \"2\",\n" +
                "    \"employee_name\": \"B\",\n" +
                "    \"employee_salary\": 250,\n" +
                "    \"employee_age\": 31,\n" +
                "    \"employee_title\": \"QA\",\n" +
                "    \"employee_email\": \"b@x.com\"\n" +
                "  } ],\n" +
                "  \"status\": \"ok\"\n" +
                "}";
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(list).addHeader("Content-Type", "application/json"));

        var response = restTemplate.getForEntity(apiUrl("/highestSalary"), Integer.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(250);
    }

    @Test
    void topTen_viaApi_ordersAndLimits() {
        String list = "{\n" +
                "  \"data\": [" +
                "{\"id\":\"1\",\"employee_name\":\"N1\",\"employee_salary\":1,\"employee_age\":30,\"employee_title\":\"T\",\"employee_email\":\"e@x.com\"}," +
                "{\"id\":\"2\",\"employee_name\":\"N2\",\"employee_salary\":2,\"employee_age\":30,\"employee_title\":\"T\",\"employee_email\":\"e@x.com\"}," +
                "{\"id\":\"3\",\"employee_name\":\"N3\",\"employee_salary\":3,\"employee_age\":30,\"employee_title\":\"T\",\"employee_email\":\"e@x.com\"}," +
                "{\"id\":\"4\",\"employee_name\":\"N4\",\"employee_salary\":4,\"employee_age\":30,\"employee_title\":\"T\",\"employee_email\":\"e@x.com\"}," +
                "{\"id\":\"5\",\"employee_name\":\"N5\",\"employee_salary\":5,\"employee_age\":30,\"employee_title\":\"T\",\"employee_email\":\"e@x.com\"}," +
                "{\"id\":\"6\",\"employee_name\":\"N6\",\"employee_salary\":6,\"employee_age\":30,\"employee_title\":\"T\",\"employee_email\":\"e@x.com\"}," +
                "{\"id\":\"7\",\"employee_name\":\"N7\",\"employee_salary\":7,\"employee_age\":30,\"employee_title\":\"T\",\"employee_email\":\"e@x.com\"}," +
                "{\"id\":\"8\",\"employee_name\":\"N8\",\"employee_salary\":8,\"employee_age\":30,\"employee_title\":\"T\",\"employee_email\":\"e@x.com\"}," +
                "{\"id\":\"9\",\"employee_name\":\"N9\",\"employee_salary\":9,\"employee_age\":30,\"employee_title\":\"T\",\"employee_email\":\"e@x.com\"}," +
                "{\"id\":\"10\",\"employee_name\":\"N10\",\"employee_salary\":10,\"employee_age\":30,\"employee_title\":\"T\",\"employee_email\":\"e@x.com\"}," +
                "{\"id\":\"11\",\"employee_name\":\"N11\",\"employee_salary\":11,\"employee_age\":30,\"employee_title\":\"T\",\"employee_email\":\"e@x.com\"}" +
                "],\n" +
                "  \"status\": \"ok\"\n" +
                "}";
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(list).addHeader("Content-Type", "application/json"));

        var response = restTemplate.getForEntity(apiUrl("/topTenHighestEarningEmployeeNames"), String[].class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().length).isEqualTo(10);
        assertThat(response.getBody()[0]).isEqualTo("N11");
    }

    @Test
    void createEmployee_viaApi_proxiesToServer() {
        String serverResponse = "{\n" +
                "  \"data\": {\n" +
                "    \"id\": \"99\",\n" +
                "    \"employee_name\": \"New\",\n" +
                "    \"employee_salary\": 500,\n" +
                "    \"employee_age\": 28,\n" +
                "    \"employee_title\": \"Eng\",\n" +
                "    \"employee_email\": \"n@x.com\"\n" +
                "  },\n" +
                "  \"status\": \"ok\"\n" +
                "}";
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(serverResponse).addHeader("Content-Type", "application/json"));

        String requestBody = "{\"name\":\"New\",\"salary\":500,\"age\":28,\"title\":\"Eng\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl(""), entity, String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("New");
    }

    @Test
    void deleteEmployee_viaApi_fetchesIdThenDeletesByName() {
        String getById = "{\n" +
                "  \"data\": {\n" +
                "    \"id\": \"abc\",\n" +
                "    \"employee_name\": \"Del Me\",\n" +
                "    \"employee_salary\": 100,\n" +
                "    \"employee_age\": 33,\n" +
                "    \"employee_title\": \"T\",\n" +
                "    \"employee_email\": \"d@x.com\"\n" +
                "  },\n" +
                "  \"status\": \"ok\"\n" +
                "}";
        String deleteResp = "{\n" +
                "  \"data\": true,\n" +
                "  \"status\": \"ok\"\n" +
                "}";
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(getById).addHeader("Content-Type", "application/json"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(deleteResp).addHeader("Content-Type", "application/json"));

        ResponseEntity<String> response = restTemplate.exchange(apiUrl("/abc"), HttpMethod.DELETE, null, String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("Del Me");
    }
}


