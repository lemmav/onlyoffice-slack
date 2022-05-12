package com.onlyoffice.slack.service.registry;

import com.onlyoffice.slack.configuration.registry.RestClientConfiguration;
import com.onlyoffice.slack.exception.OnlyofficeRegistryResponseException;
import com.onlyoffice.slack.exception.UnableToPerformSlackOperationException;
import com.onlyoffice.slack.model.registry.User;
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
class OnlyofficeUserRegistryService {
    private final RestClientConfiguration clientConfiguration;
    private final RestTemplate restClient;

    @Retryable(
            value = {
                    ResourceAccessException.class,
                    ConnectException.class,
                    ConnectionPoolTimeoutException.class,
                    OnlyofficeRegistryResponseException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(random = true, delay = 250, maxDelay = 600)
    )
    @CircuitBreaker(name = "onlyofficeRegistryQueryService", fallbackMethod = "getUserFallback")
    public User getUser(String wid, String uid)
            throws OnlyofficeRegistryResponseException {
        log.debug("New registry request to get user with id = {} and workspace id = {}", uid, wid);

        ResponseEntity<User> response = restClient.getForEntity(String.format(
                "%s/v1/workspace/%s/%s/user/%s", clientConfiguration.getResourceServer(),
                clientConfiguration.getWorkspaceType(), wid, uid
        ), User.class);

        HttpStatus status = response.getStatusCode();
        if (status == HttpStatus.BAD_REQUEST) {
            log.warn("An error has occurred while trying to get a user instance");
            return null;
        }

        if (!status.is2xxSuccessful())
            throw new OnlyofficeRegistryResponseException(
                    "An error has occurred while trying to get a user instance: " + response
                            .getStatusCode().name() + response.getStatusCode().getReasonPhrase());

        return response.getBody();
    }

    private User getUserFallback(String wid, String uid, Throwable t)
            throws OnlyofficeRegistryResponseException {
        log.error("Circuit breaker has terminated get user {} call", uid);
        return null;
    }

    @Retryable(
            value = {
                    ResourceAccessException.class,
                    ConnectException.class,
                    ConnectionPoolTimeoutException.class,
                    OnlyofficeRegistryResponseException.class
            },
            maxAttempts = 6,
            backoff = @Backoff(random = true, delay = 200, maxDelay = 950)
    )
    @CircuitBreaker(name = "onlyofficeRegistryQueryService", fallbackMethod = "deleteUserFallback")
    public void deleteUser(String wid, String uid)
            throws UnableToPerformSlackOperationException {
        log.debug("New registry request to delete user with id = {} and workspace id = {}", uid, wid);

        restClient.delete(String.format(
                "%s/v1/workspace/%s/%s/user/%s", clientConfiguration.getResourceServer(),
                clientConfiguration.getWorkspaceType(), wid, uid
        ));
    }

    private void deleteUserFallback(String wid, String uid, Throwable t)
            throws OnlyofficeRegistryResponseException {
        log.error("Circuit breaker has terminated delete user {} call", uid);
        throw new OnlyofficeRegistryResponseException("Could not delete user " +
                uid + " for workspace " + wid + " due to circuit breaker termination");
    }
}
