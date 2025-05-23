package com.ai_tool_calling;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;

public class ReflectiveToolDispatcher {

    private final ToolRegistry registry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReflectiveToolDispatcher(ToolRegistry registry) {
        this.registry = registry;
    }

    public String dispatch(ToolCall toolCall) throws Exception {
        String toolName =  toolCall.getName();

        ToolRegistry.ToolHandler toolHandler = registry.getToolHandler(toolName);

        Method method = toolHandler.getMethod();

        if(!method.isAnnotationPresent(Tool.class)) {
            throw new Exception("Tool '" + toolName + "' is not annotated with Tool");
        }

        // Get the parameter type of the method (Should be POJO of the Tool)
        Class<?> paramType = method.getParameterTypes()[0];

        // Convert arguments Map to JSON, then to the POJO paramType

        String argsJson = objectMapper.writeValueAsString(toolCall.getArguments());
        Object argsPojo = objectMapper.readValue(argsJson, paramType);

        Object result = method.invoke(toolHandler.getProvider(), argsPojo);

        return (String) result;

    }

    private Method findMethodByName(Class<?> clazz, String methodName) throws NoSuchMethodException {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(methodName) && m.isAnnotationPresent(Tool.class)) {
                return m;
            }
        }
        throw new NoSuchMethodException("No tool method found for: " + methodName);
    }

}
