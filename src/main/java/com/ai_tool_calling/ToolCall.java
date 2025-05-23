package com.ai_tool_calling;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

// Removed 'import java.util.Map;' as 'arguments' is now Object,
// but it will likely be a Map<String, Object> or String after deserialization
// from the LLM, or a specific POJO type.

@Getter
@Setter
public class ToolCall { // This POJO now represents what you expect from the LLM
    private String id;
    private String type; // e.g., "function"
    private String description;

    @JsonProperty("function")
    private FunctionDetails function;

    @Getter
    @Setter
    public static class FunctionDetails {
        private String name;
        private String arguments;
    }
}