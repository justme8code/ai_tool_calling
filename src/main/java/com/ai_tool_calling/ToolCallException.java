package com.ai_tool_calling;

public class ToolCallException extends RuntimeException { // Or extends Exception if you want checked exceptions

    public ToolCallException(String message) {
        super(message);
    }

    public ToolCallException(String message, Throwable cause) {
        super(message, cause);
    }
}