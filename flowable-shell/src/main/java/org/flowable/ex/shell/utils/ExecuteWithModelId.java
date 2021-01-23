package org.flowable.ex.shell.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.impl.client.CloseableHttpClient;

@FunctionalInterface
public interface ExecuteWithModelId {
    JsonNode execute(CloseableHttpClient client, String modelId);
}
