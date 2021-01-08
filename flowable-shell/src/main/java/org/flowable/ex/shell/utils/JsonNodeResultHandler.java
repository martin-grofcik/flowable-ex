package org.flowable.ex.shell.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.shell.result.TerminalAwareResultHandler;

public class JsonNodeResultHandler extends TerminalAwareResultHandler<JsonNode> {

    @Override
    protected void doHandleResult(JsonNode jsonNode) {
        this.terminal.writer().println(jsonNode.toPrettyString());
        this.terminal.writer().flush();
    }
}
