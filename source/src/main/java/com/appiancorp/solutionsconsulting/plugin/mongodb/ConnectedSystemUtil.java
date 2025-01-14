package com.appiancorp.solutionsconsulting.plugin.mongodb;

import com.appian.connectedsystems.templateframework.sdk.IntegrationError;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import com.appian.connectedsystems.templateframework.sdk.diagnostics.IntegrationDesignerDiagnostic;
import com.appiancorp.solutionsconsulting.plugin.mongodb.exceptions.InvalidJsonException;
import com.mongodb.internal.build.MongoDriverVersion;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;


@SuppressWarnings("unused")
public class ConnectedSystemUtil {
    private Long startTime;
    private Long endTime;
    private final HashMap<String, Object> requestDiagnostics;
    private final HashMap<String, Object> responseDiagnostics;
    private final HashMap<String, Object> response;

    public ConnectedSystemUtil(String method) {
        requestDiagnostics = new HashMap<>();
        responseDiagnostics = new HashMap<>();
        responseDiagnostics.put("MongoDB Java Sync Driver Version", MongoDriverVersion.VERSION);
        responseDiagnostics.put("Method Used", method);
        response = new HashMap<>();
    }


    public void startTiming() {
        startTime = System.currentTimeMillis();
    }

    public void stopTiming() {
        endTime = System.currentTimeMillis();
    }

    public long getTiming() {
        if (startTime == null || endTime == null) {
            return -1;
        }
        return endTime - startTime;
    }

    public void addRequestDiagnostic(String key, Object value) {
        requestDiagnostics.put(key, value);
    }

    public void addAllRequestDiagnostic(Map<String, Object> map) {
        requestDiagnostics.putAll(map);
    }

    public HashMap<String, Object> getRequestDiagnostics() {
        return requestDiagnostics;
    }


    public void addResponseDiagnostic(String key, Object value) {
        responseDiagnostics.put(key, value);
    }

    public void addAllResponseDiagnostic(Map<String, Object> map) {
        responseDiagnostics.putAll(map);
    }

    public HashMap<String, Object> getResponseDiagnostics() {
        return responseDiagnostics;
    }

    public void addResponse(String key, Object value) {
        response.put(key, value);
    }

    public void addAllResponse(Map<String, Object> map) {
        response.putAll(map);
    }

    public IntegrationResponse buildSuccess() {
        IntegrationResponse.Builder integrationResponseBuilder = IntegrationResponse.forSuccess(getResponse());
        IntegrationDesignerDiagnostic integrationDesignerDiagnostic = IntegrationDesignerDiagnostic.builder()
                .addRequestDiagnostic(getRequestDiagnostics())
                .addResponseDiagnostic(getResponseDiagnostics())
                .addExecutionTimeDiagnostic(getTiming())
                .build();
        return integrationResponseBuilder.withDiagnostic(integrationDesignerDiagnostic).build();
    }

    public HashMap<String, Object> getResponse() {
        return response;
    }


    public IntegrationResponse buildApiExceptionError(Exception e) {
        if (e instanceof InvalidJsonException) {
            return buildApiExceptionError(
                    "Invalid Json Exception",
                    e.getMessage(),
                    ((InvalidJsonException) e).jsonString
            );
        } else {
            String message = e.getMessage();
            String detail = "";
            if (message.contains("The full response is")) {
                detail = message.replaceAll("^.* The full response is ", "");
                message = message.replaceAll(" The full response is .*$", "");
            }
            return buildApiExceptionError(
                    String.join(" ", StringUtils.splitByCharacterTypeCamelCase(e.getClass().getSimpleName())),
                    message,
                    detail
            );
        }
    }

    public IntegrationResponse buildApiExceptionError(String title, String message, String detail) {
        IntegrationDesignerDiagnostic diagnostics = IntegrationDesignerDiagnostic.builder()
                .addRequestDiagnostic(requestDiagnostics)
                .addResponseDiagnostic(responseDiagnostics)
                .build();
        IntegrationError integrationError = IntegrationError.builder()
                .title(title)
                .message(message)
                .detail(detail)
                .build();
        IntegrationResponse.Builder integrationResponseBuilder = IntegrationResponse.forError(integrationError).withDiagnostic(diagnostics);
        return integrationResponseBuilder.build();
    }
}
