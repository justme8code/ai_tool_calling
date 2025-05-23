
# ToolCaller

A simple Java tool invocation system inspired by modern AI tool calling protocols.  
Allows invoking annotated tool methods reflectively using JSON-like input — just like how AI agents call external tools.

---

## What is ToolCaller?

ToolCaller lets you:

- Define **tool methods** annotated with `@Tool`
- Pass tool calls as `{ "name": "...", "arguments": {...} }` objects
- Automatically map arguments to typed POJOs
- Dispatch calls to registered tool providers dynamically

It’s basically a minimal, pluggable framework for enabling AI-like tool calling in Java.

---

## Why?

Inspired by the new way AI models call external tools by sending JSON requests describing the tool name and args.  
Wanted to experiment and build a **personalized tool-calling system** with Java reflection + Jackson.

---

## Quickstart

### 1. Define your tool method

```java
public class ToolSet {
    @Tool
    public String run_shell_command(RunShellCommandArgs args) {
        return "Ran command: " + args.getCommand();
    }
}
```

### 2. Define your argument POJO

```java
public class RunShellCommandArgs {
    private String command;
    // getters & setters
}
```

### 3. Register your tool provider

```java
ToolRegistry registry = new ToolRegistry();
registry.registerToolProvider(new ToolSet());
```

### 4. Dispatch tool calls
 A full Example:
```java

package com.ai_tool_calling;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {

    public static void main(String[] args) throws Exception { // Or handle JsonProcessingException
        ToolRegistry toolRegistry = new ToolRegistry();
        ToolSet toolSet = new ToolSet(); // Your ToolSet with callCommand
        // To test tool execution error:
        // toolSet.setShouldThrowError(true); // Add a flag to ToolSet to make callCommand throw an error
        toolRegistry.registerToolProvider(toolSet);

        ObjectMapper objectMapper = new ObjectMapper();

        // --- Simulate LLM sending a ToolCall request ---
        RunShellCommandArgs argsForTool = new RunShellCommandArgs();
        argsForTool.setCommand("ls -l");
        // To test argument parsing error:
        // String malformedArgsJson = "{\"command_typo\": \"ls -l\"}";

        ToolCall llmRequest = new ToolCall();
        llmRequest.setId("call_test_789");
        llmRequest.setType("function");

        ToolCall.FunctionDetails functionDetails = new ToolCall.FunctionDetails();
        functionDetails.setName("callComm");
        functionDetails.setArguments(objectMapper.writeValueAsString(argsForTool));
        // functionDetails.setArguments(malformedArgsJson); // For testing arg parsing error

        llmRequest.setFunction(functionDetails);
        // --- End of LLM request simulation ---

        ReflectiveToolDispatcher toolDispatcher = new ReflectiveToolDispatcher(toolRegistry);

        // Dispatch the call and get the result object.
        // No try-catch needed around dispatch() itself if it always returns ToolCallResult.
        ToolCallResult resultForLLM = toolDispatcher.dispatch(llmRequest);

        // This resultForLLM is what you'd serialize and send back to the LLM API
        String jsonResponseToLLM = objectMapper.writeValueAsString(resultForLLM);

        System.out.println("JSON to send back to LLM:");
        System.out.println(jsonResponseToLLM);

        // You can check the content to see if it was a success or an error message
        if (resultForLLM.getContent() instanceof String &&
                ((String) resultForLLM.getContent()).toLowerCase().contains("error")) {
            System.out.println("\nAn error occurred during tool call.");
        } else {
            System.out.println("\nTool call likely succeeded.");
        }
    }
}
```

---

## How it works under the hood

- `ToolRegistry` keeps track of all tool providers (sets of annotated methods).
- `ReflectiveToolDispatcher` finds the right tool method by name and checks for the `@Tool` annotation.
- It converts the JSON-like arguments to the proper POJO type using Jackson.
- Calls the method reflectively, then returns the output.

---

## Future ideas

- Plugin system to load tools dynamically
- Async / streaming tool calls
- Tool metadata for richer tool discovery
- Secure sandboxing for system commands

---

## License

MIT License — built with ❤️ by Thompson

---

## Feedback?

Hit me up if you want to collab or improve this!  
Inspired by how AI models are leveling up tool integrations — time for us devs to catch up 😎# ai_tool_calling
# ai_tool_calling
