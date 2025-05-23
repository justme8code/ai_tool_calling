package com.ai_tool_calling;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RunShellCommandArgs {

    private String command;

    public RunShellCommandArgs() {}

    public RunShellCommandArgs(String command) {
        this.command = command;
    }

}
