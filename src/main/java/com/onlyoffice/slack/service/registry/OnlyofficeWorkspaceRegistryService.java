package com.onlyoffice.slack.service.registry;

import com.onlyoffice.slack.configuration.registry.RestClientConfiguration;
import com.onlyoffice.slack.exception.OnlyofficeRegistryResponseException;
import com.onlyoffice.slack.exception.UnableToPerformSlackOperationException;
import com.onlyoffice.slack.model.registry.GenericResponse;
import com.onlyoffice.slack.model.registry.User;
import com.onlyoffice.slack.model.registry.Workspace;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
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
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
class OnlyofficeWorkspaceRegistryService {
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
            backoff = @Backoff(random = true, delay = 200, maxDelay = 600)
    )
    @CircuitBreaker(name = "onlyofficeRegistryQueryService", fallbackMethod = "getWorkspaceCircuitFallback")
    public Workspace getWorkspace(String wid)
            throws UnableToPerformSlackOperationException {
        log.debug("New registry request to get workspace with id = {}", wid);

        ResponseEntity<Workspace> response = restClient.getForEntity(String
                        .format("%s/v1/workspace/%s/%s", clientConfiguration.getResourceServer(),
                                clientConfiguration.getWorkspaceType(), wid),
                        Workspace.class
        );

        HttpStatus status = response.getStatusCode();
        if (status == HttpStatus.BAD_REQUEST) {
            log.warn("An attempt to get an invalid workspace instance: " +
                    response.getStatusCode().getReasonPhrase());
            return null;
        }

        if (!status.is2xxSuccessful())
            throw new OnlyofficeRegistryResponseException(
                    "Workspace fetching error: " + response.getStatusCode().name() + " " + response.getStatusCode().getReasonPhrase());

        return response.getBody();
    }

    public Workspace getWorkspaceCircuitFallback(String wid, CallNotPermittedException e) {
        log.error("Current get workspace {} call has been terminated by CircuitBreaker", wid);
        return null;
    }

    @Retryable(
            value = {
                    ResourceAccessException.class,
                    ConnectException.class,
                    ConnectionPoolTimeoutException.class,
                    OnlyofficeRegistryResponseException.class
            },
            maxAttempts = 1,
            backoff = @Backoff(random = true, delay = 500, maxDelay = 1500)
    )
    @CircuitBreaker(name = "onlyofficeRegistryCommandService", fallbackMethod = "saveWorkspaceAndUserFallback")
    public void saveWorkspaceAndUser(Workspace workspace, User user, User bot)
            throws UnableToPerformSlackOperationException {
        log.debug("New registry request to save workspace with id = {}, uid = {} and bot = {}",
                workspace.getId(), user.getId(), bot.getId());

        Function<User, ResponseEntity<User>> persistUser = (u) -> restClient.postForEntity(
                    String.format("%s/v1/workspace/%s/%s/user", clientConfiguration.getResourceServer(),
                            clientConfiguration.getWorkspaceType(), workspace.getId()),
                    u, User.class);

        ResponseEntity<GenericResponse> botResponse = restClient.getForEntity(String.format(
                "%s/v1/workspace/%s/%s/user/%s", clientConfiguration.getResourceServer(),
                clientConfiguration.getWorkspaceType(),
                workspace.getId(), bot.getId()
        ), GenericResponse.class);

        HttpStatus botStatus = botResponse.getStatusCode();
        String message = botResponse.getBody().getMessage();
        if (botStatus == HttpStatus.BAD_REQUEST && message != null && !message.isBlank() && !botResponse.getBody().getSuccess()) {
            restClient.postForEntity(
                    String.format("%s/v1/workspace/%s", clientConfiguration.getResourceServer(),
                            clientConfiguration.getWorkspaceType()),
                    workspace, Workspace.class
            );

            ResponseEntity<User> botPersistResponse = persistUser.apply(bot);
            if (!botPersistResponse.getStatusCode().is2xxSuccessful())
                throw new UnableToPerformSlackOperationException("Bot user persistence error: " + botPersistResponse
                        .getStatusCode().name() + botPersistResponse.getStatusCode().getReasonPhrase());
        }

        ResponseEntity<User> userPersistResponse = persistUser.apply(user);
        if (!userPersistResponse.getStatusCode().is2xxSuccessful())
            throw new UnableToPerformSlackOperationException("User persistence error: " + userPersistResponse
                    .getStatusCode().name() + userPersistResponse.getStatusCode().getReasonPhrase());
    }

    private void saveWorkspaceAndUserFallback(Workspace workspace, User user, User bot, Throwable t)
            throws UnableToPerformSlackOperationException {
        log.error("Circuit breaker has terminated save workspace {} and user {} call",
                workspace.getId(), user.getId());
        throw new UnableToPerformSlackOperationException("Could not save workspace due to circuit breaker termination");
    }

    @Retryable(
            value = {
                    ResourceAccessException.class,
                    ConnectException.class,
                    ConnectionPoolTimeoutException.class
            },
            maxAttempts = 6,
            backoff = @Backoff(random = true, delay = 200, maxDelay = 1000)
    )
    @CircuitBreaker(name = "onlyofficeRegistryQueryService", fallbackMethod = "deleteWorkspaceFallback")
    public void deleteWorkspace(String wid)
            throws UnableToPerformSlackOperationException {
        log.debug("New registry request to delete workspace with id = {}", wid);
        restClient.delete(String
                .format("%s/v1/workspace/%s/%s", clientConfiguration.getResourceServer(),
                        clientConfiguration.getWorkspaceType(), wid));
    }

    private void deleteWorkspaceFallback(String wid, Throwable t)
            throws UnableToPerformSlackOperationException {
        log.error("Circuit breaker has terminated delete workspace {} call", wid);
        throw new UnableToPerformSlackOperationException("Could not delete workspace due to circuit breaker termination");
    }
}
