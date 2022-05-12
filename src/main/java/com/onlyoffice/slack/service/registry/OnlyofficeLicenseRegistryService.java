package com.onlyoffice.slack.service.registry;

import com.onlyoffice.slack.configuration.registry.RestClientConfiguration;
import com.onlyoffice.slack.exception.OnlyofficeRegistryResponseException;
import com.onlyoffice.slack.model.registry.DemoInfo;
import com.onlyoffice.slack.model.registry.GenericResponse;
import com.onlyoffice.slack.model.registry.License;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.ConnectException;

@Service
@RequiredArgsConstructor
@Slf4j
class OnlyofficeLicenseRegistryService {
    private final RestClientConfiguration clientConfiguration;
    private final RestTemplate restClient;

    @Retryable(
            value = {
                    ResourceAccessException.class,
                    ConnectException.class,
                    ConnectionPoolTimeoutException.class,
                    OnlyofficeRegistryResponseException.class
            },
            maxAttempts = 2,
            backoff = @Backoff(random = true, delay = 300, maxDelay = 850)
    )
    @CircuitBreaker(name = "onlyofficeRegistryCommandService", fallbackMethod = "saveLicenseFallback")
    public GenericResponse saveLicense(String wid, License license)
            throws OnlyofficeRegistryResponseException {
        log.debug("New registry license update request with workspace id = {}", wid);

        ResponseEntity<GenericResponse> response = restClient.postForEntity(
                String.format("%s/v1/workspace/%s/%s/license",
                        clientConfiguration.getResourceServer(), clientConfiguration.getWorkspaceType(), wid),
                license, GenericResponse.class
        );

        HttpStatus status = response.getStatusCode();
        if (status == HttpStatus.BAD_REQUEST) {
            String message = "An error has occurred while trying to persist a license instance: "
                    + response.getBody().getMessage();
            log.warn(message);
            return GenericResponse
                    .builder()
                    .success(false)
                    .message(message)
                    .build();
        }

        if (!status.is2xxSuccessful())
            throw new OnlyofficeRegistryResponseException("An error has occurred while trying to persist a license instance: "
                    + status.name() + status.getReasonPhrase());

        return response.getBody();
    }

    private GenericResponse saveLicenseFallback(String wid, License license, Throwable t)
            throws OnlyofficeRegistryResponseException {
        log.error("Circuit breaker has terminated save license {} call", wid);
        return GenericResponse
                .builder()
                .success(false)
                .message("Circuit breaker has terminated save license call for " + wid)
                .build();
    }

    @Retryable(
            value = {
                    ResourceAccessException.class,
                    ConnectException.class,
                    ConnectionPoolTimeoutException.class,
                    OnlyofficeRegistryResponseException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(random = true, delay = 200, maxDelay = 750)
    )
    @CircuitBreaker(name = "onlyofficeRegistryQueryService", fallbackMethod = "startDemoFallback")
    public GenericResponse startDemo(String wid)
            throws OnlyofficeRegistryResponseException {
        log.debug("New registry start demo request with workspace id = {}", wid);

        ResponseEntity<GenericResponse> response = restClient.postForEntity(
                String.format("%s/v1/workspace/%s/%s/demo",
                        clientConfiguration.getResourceServer(), clientConfiguration.getWorkspaceType(), wid),
                null, GenericResponse.class
        );

        HttpStatus status = response.getStatusCode();
        if (status == HttpStatus.BAD_REQUEST) {
            String message = "An error has occurred while trying to persist a new demo instance: " +
                    response.getBody().getMessage();
            log.warn(message);
            return GenericResponse
                    .builder()
                    .message(message)
                    .success(false)
                    .build();
        }

        if (!status.is2xxSuccessful())
            throw new OnlyofficeRegistryResponseException("An error has occurred while trying to persist a new demo instance: "
                    + response.getStatusCode().name() + response.getStatusCode().getReasonPhrase());

        return response.getBody();
    }

    private GenericResponse startDemoFallback(String wid, Throwable t)
            throws OnlyofficeRegistryResponseException {
        log.error("Circuit breaker has terminated start demo {} call", wid);
        return GenericResponse
                .builder()
                .message("Circuit breaker has terminated start demo call for " + wid)
                .success(false)
                .build();
    }

    @Retryable(
            value = {
                    ResourceAccessException.class,
                    ConnectException.class,
                    ConnectionPoolTimeoutException.class,
                    OnlyofficeRegistryResponseException.class
            },
            maxAttempts = 5,
            backoff = @Backoff(random = true, delay = 200, maxDelay = 450)
    )
    @CircuitBreaker(name = "onlyofficeRegistryQueryService", fallbackMethod = "getDemoFallback")
    public DemoInfo getDemo(String wid)
            throws OnlyofficeRegistryResponseException {
        log.debug("New registry get demo request with workspace id = {}", wid);

        ResponseEntity<DemoInfo> response = restClient.getForEntity(
                String.format("%s/v1/workspace/%s/%s/demo",
                        clientConfiguration.getResourceServer(), clientConfiguration.getWorkspaceType(), wid),
                DemoInfo.class
        );

        HttpStatus status = response.getStatusCode();
        if (status == HttpStatus.BAD_REQUEST) {
            log.warn("An error has occurred while trying to get a demo instance: " +
                    response.getStatusCode().getReasonPhrase());
            return null;
        }

        if (!status.is2xxSuccessful())
            throw new OnlyofficeRegistryResponseException("An error has occurred while trying to get a demo instance: "
                    + response.getStatusCode().name() + response.getStatusCode().getReasonPhrase());

        return response.getBody();
    }

    private DemoInfo getDemoFallback(String wid, Throwable t)
            throws OnlyofficeRegistryResponseException {
        log.error("Circuit breaker has terminated get demo {} call", wid);
        return null;
    }
}
