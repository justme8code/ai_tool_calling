package com.ai_tool_calling;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ToolCall {
    private String name;
    private Map<String,Object> arguments;
}
