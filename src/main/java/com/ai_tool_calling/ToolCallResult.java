package com.ai_tool_calling;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ToolCallResult {
    private String role;
    @JsonProperty("tool_call_id")
    private String toolCallId;
    private Object content;
}
