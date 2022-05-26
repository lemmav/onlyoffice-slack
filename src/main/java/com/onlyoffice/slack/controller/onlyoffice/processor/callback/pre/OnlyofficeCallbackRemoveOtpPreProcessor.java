package com.onlyoffice.slack.controller.onlyoffice.processor.callback.pre;

import com.google.common.collect.ImmutableMap;
import com.onlyoffice.slack.model.onlyoffice.OnlyofficeCallbackToken;
import com.onlyoffice.slack.model.slack.Caller;
import com.onlyoffice.slack.service.slack.SlackOtpGeneratorService;
import core.model.callback.Callback;
import core.processor.preprocessor.OnlyofficeCallbackPreProcessor;
import exception.OnlyofficeInvalidParameterRuntimeException;
import exception.OnlyofficeProcessBeforeRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OnlyofficeCallbackRemoveOtpPreProcessor extends OnlyofficeCallbackPreProcessor<OnlyofficeCallbackToken> {
    private final SlackOtpGeneratorService otpGeneratorService;

    public OnlyofficeCallbackToken validateSchema(Map<String, Object> customData, ImmutableMap<String, Object> schema) {
        try {
            log.debug("validating callback token");
            OnlyofficeCallbackToken token = (OnlyofficeCallbackToken) customData.get("callbackToken");
            if (token == null)
                throw new ClassCastException("Expected to get an OnlyofficeCalbackToken instance. Got null");
            return token;
        } catch (ClassCastException e) {
            throw new OnlyofficeProcessBeforeRuntimeException(e.getMessage());
        }
    }

    public void processBefore(Callback callback, OnlyofficeCallbackToken token) throws OnlyofficeProcessBeforeRuntimeException, OnlyofficeInvalidParameterRuntimeException {
        Integer status = callback.getStatus();

        boolean validOtp = otpGeneratorService.validateScheduledOtp(token.getOtpAt(),
                token.getOtpChannel(), Caller
                        .builder()
                            .wid(token.getWorkspace())
                            .id(token.getUser())
                            .name(token.getUser())
                        .build()
        );
        if (!validOtp)
            throw new OnlyofficeProcessBeforeRuntimeException("Could not validate an OTP");

        if (status.equals(2) || status.equals(4))
            otpGeneratorService.removeScheduledOtp(token.getOtpCode(), Caller
                    .builder()
                    .wid(token.getWorkspace())
                    .id(token.getUser())
                    .name(token.getUser())
                    .build()
            );
    }

    public String preprocessorName() {
        return "onlyoffice.preprocessor.slack.callback.otp";
    }
}
