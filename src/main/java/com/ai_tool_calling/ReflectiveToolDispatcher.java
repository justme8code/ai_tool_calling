package com.ai_tool_calling;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public class ReflectiveToolDispatcher {

    private final ToolRegistry registry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReflectiveToolDispatcher(ToolRegistry registry) {
        this.registry = registry;
    }

    public ToolCallResult dispatch(ToolCall llmToolCall) { // No longer throws generic Exception from signature
        String toolCallIdFromLLM = null;
        String toolNameFromLLM = null;

        try {
            if (llmToolCall == null || llmToolCall.getFunction() == null) {
                throw new ToolCallException("ToolCall request or its function details cannot be null.");
            }

            toolCallIdFromLLM = llmToolCall.getId(); // Get ID early for error reporting
            if (toolCallIdFromLLM == null || toolCallIdFromLLM.trim().isEmpty()){
                // While LLMs usually provide it, good to be defensive for manual tests
                System.err.println("Warning: tool_call_id is missing in the request.");
                toolCallIdFromLLM = "unknown_id_" + System.currentTimeMillis(); // Assign a temp ID
            }

            ToolCall.FunctionDetails functionDetails = llmToolCall.getFunction();
            toolNameFromLLM = functionDetails.getName();
            String argumentsJsonString = functionDetails.getArguments();

            if (toolNameFromLLM == null || toolNameFromLLM.trim().isEmpty()) {
                throw new ToolCallException("Tool name cannot be null or empty in function details.");
            }

            ToolRegistry.ToolHandler toolHandler = registry.getToolHandler(toolNameFromLLM);
            if (toolHandler == null) {
                throw new ToolCallException("No tool registered with name: " + toolNameFromLLM);
            }

            Method methodToExecute = getMethodAndValidate(toolHandler, toolNameFromLLM);

            Object argsPojo = null;
            if (methodToExecute.getParameterCount() > 0) {
                if (argumentsJsonString == null || argumentsJsonString.trim().isEmpty()) {
                    throw new ToolCallException("Tool '" + toolNameFromLLM + "' expects arguments, but arguments JSON string is missing or empty.");
                }
                Class<?> expectedArgType = methodToExecute.getParameterTypes()[0];
                try {
                    argsPojo = objectMapper.readValue(argumentsJsonString, expectedArgType);
                } catch (JsonProcessingException e) {
                    throw new ToolCallException("Failed to parse arguments JSON for tool '" + toolNameFromLLM + "'. Expected format for " + expectedArgType.getSimpleName() + ". JSON: " + argumentsJsonString, e);
                }
            } else {
                if (argumentsJsonString != null && !argumentsJsonString.trim().isEmpty() && !argumentsJsonString.trim().equals("{}")) {
                    System.err.println("Warning: Tool '" + toolNameFromLLM + "' takes no arguments, but arguments JSON was provided: " + argumentsJsonString);
                }
            }

            Object executionContent;
            try {
                if (argsPojo != null) {
                    executionContent = methodToExecute.invoke(toolHandler.getProvider(), argsPojo);
                } else {
                    executionContent = methodToExecute.invoke(toolHandler.getProvider());
                }
            } catch (IllegalAccessException e) {
                throw new ToolCallException("Cannot access method for tool '" + toolNameFromLLM + "'.", e);
            } catch (InvocationTargetException e) {
                // Exception occurred *inside* the tool method itself
                Throwable cause = e.getCause();
                // Let this specific exception propagate to be caught by the outer catch block
                // which is designed to create an error ToolCallResult.
                // Or, create a more specific message here:
                // throw new ToolCallException("Execution of tool '" + toolNameFromLLM + "' failed: " + cause.getMessage(), cause);
                // For now, let it be caught by the outer catch.
                // To provide the original exception from the tool to the error result:
                return createErrorToolCallResult("tool", toolCallIdFromLLM,
                        "Execution of tool '" + toolNameFromLLM + "' failed: " + cause.getMessage(),
                        toolNameFromLLM);
            }

            return createSuccessToolCallResult("tool", toolCallIdFromLLM, executionContent, toolNameFromLLM);

        } catch (ToolCallException e) { // Catch our specific exceptions
            System.err.println("ToolCallException: " + e.getMessage());
            // If toolCallIdFromLLM is null here, it means the exception occurred very early.
            // It's important that toolCallIdFromLLM is available for error reporting.
            String idForError = (toolCallIdFromLLM != null) ? toolCallIdFromLLM : "unknown_id_early_error";
            String nameForError = (toolNameFromLLM != null) ? toolNameFromLLM : "unknown_tool";
            return createErrorToolCallResult("tool", idForError, e.getMessage(), nameForError);
        } catch (Exception e) { // Catch any other unexpected exceptions during dispatch
            System.err.println("Unexpected dispatch error: " + e.getMessage());
            // e.printStackTrace(); // Good for debugging these unexpected ones
            String idForError = (toolCallIdFromLLM != null) ? toolCallIdFromLLM : "unknown_id_unexpected_error";
            String nameForError = (toolNameFromLLM != null) ? toolNameFromLLM : "unknown_tool";
            return createErrorToolCallResult("tool", idForError, "Unexpected internal error during dispatch: " + e.getMessage(), nameForError);
        }
    }

    private Method getMethodAndValidate(ToolRegistry.ToolHandler toolHandler, String toolNameFromLLM) throws ToolCallException {
        Method methodToExecute = toolHandler.getMethod();
        if (!methodToExecute.isAnnotationPresent(Tool.class)) {
            // This should ideally be caught during registration, but good to ensure.
            throw new ToolCallException("Method for tool '" + toolNameFromLLM + "' is not annotated with @Tool, inconsistent registration state.");
        }
        return methodToExecute;
    }

    private ToolCallResult createSuccessToolCallResult(String role, String toolCallId, Object content, String toolName) {
        // You might add toolName to ToolCallResult if your LLM API needs 'name' in the tool response message.
        // For OpenAI, 'tool_call_id' and 'content' are the primary fields in the tool message.
        return new ToolCallResult(role, toolCallId, content);
    }

    private ToolCallResult createErrorToolCallResult(String role, String toolCallId, String errorMessage, String toolName) {
        // For OpenAI, the 'content' of a tool error response is typically just a string.
        // So, errorMessage itself becomes the content.
        // If you wanted a structured error, content would be Map.of("error", errorMessage, "tool_name", toolName)
        // and ToolCallResult.content would be Object.
        return new ToolCallResult(role, toolCallId, errorMessage);
    }
}