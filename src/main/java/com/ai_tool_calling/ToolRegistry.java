package com.ai_tool_calling;

import lombok.Getter;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ToolRegistry {
    private final Map<String, ToolHandler> tools = new HashMap<>();

    public void registerToolProvider(ToolProvider provider) {
        Class<?> clazz = provider.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Tool.class)) {
                String toolName = method.getName();
                tools.put(toolName, new ToolHandler(provider, method));
            }
        }
    }

    public ToolHandler getToolHandler(String name) {
        return tools.get(name);
    }

    public boolean isRegistered(String name) {
        return tools.containsKey(name);
    }

    @Getter
    public static class ToolHandler {
        private final Object provider;
        private final Method method;

        public ToolHandler(ToolProvider provider, Method method) {
            this.provider = provider;
            this.method = method;
        }

    }
}
