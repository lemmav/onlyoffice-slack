package com.onlyoffice.slack.configuration.registry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

@Configuration
@Slf4j
public class RestTemplateErrorHandlerConfigurer implements ResponseErrorHandler {
    public boolean hasError(ClientHttpResponse response) throws IOException {
        HttpStatus.Series status = response.getStatusCode().series();
        return status == HttpStatus.Series.CLIENT_ERROR || status == HttpStatus.Series.SERVER_ERROR;
    }

    public void handleError(ClientHttpResponse response) throws IOException {
        HttpStatus.Series status = response.getStatusCode().series();
        if (status == HttpStatus.Series.SERVER_ERROR)
            log.error("ONLYOFFICE registry is busy: {}", response.getStatusCode().value());
        if (status == HttpStatus.Series.CLIENT_ERROR)
            log.debug("ONLYOFFICE registry responded with status: {}", response.getStatusCode().value());
    }
}
