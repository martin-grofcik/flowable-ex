package org.flowable.ex.shell.commands;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.flowable.ex.shell.utils.RestCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.util.StringUtils;

import java.net.URISyntaxException;
import java.nio.file.Paths;

@ShellCommandGroup
@ShellComponent
public class Deployment extends RestCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(Deployment.class);

    @ShellMethod("Deploy given application")
    public JsonNode deploy(String pathToApplication,
                           @ShellOption(defaultValue = "") String deploymentName,
                           @ShellOption(defaultValue = "") String tenantId) {
        String mandatoryFileName = StringUtils.isEmpty(deploymentName) ? Paths.get(pathToApplication).getFileName().toString() : deploymentName;

        return executeWithClient(client -> {
            HttpPost httpPost = new HttpPost(configuration.getRestURL() + "app-api/app-repository/deployments");
            return uploadFile(client, pathToApplication, mandatoryFileName, mandatoryFileName, tenantId, httpPost);
        });
    }

    @ShellMethod(value = "Delete all deployments with given name, tenantId from runtime. WARNING - use only for testing purposes",
            key ={"rmd", "delete-deployments"})
    public void deleteDeployments(String name, @ShellOption(defaultValue = "") String tenantId) {
        executeWithClient(client -> deleteDeployments(client, name, tenantId));
    }

    @ShellMethod(value = "list deployments", key = {"list-deployments", "lsd"})
    public JsonNode listDeployments(@ShellOption(defaultValue = "") String name, @ShellOption(defaultValue = "") String tenantId) {
        return executeWithClient(client -> getDeployments(client, name, tenantId));
    }

    protected void deleteDeployment(CloseableHttpClient client, String deploymentId){
        try {
            LOGGER.info("Deleting deployment id {}.", deploymentId);
            URIBuilder uriBuilder = new URIBuilder(configuration.getRestURL() + "app-api/app-repository/deployments/" + deploymentId);
            HttpDelete httpDelete = new HttpDelete(uriBuilder.build());
            CloseableHttpResponse response = executeBinaryRequest(client, httpDelete, false);
            try {
                LOGGER.info("Delete response {}", response.getStatusLine());
            } finally {
                closeResponse(response);
            }
        } catch (URISyntaxException e) {
            LOGGER.error("Unable to deleteDeployment.", e);
        }
    }

    protected JsonNode deleteDeployments(CloseableHttpClient client, String name, @ShellOption(defaultValue = "") String tenantId) {
        JsonNode deployments = getDeployments(client, name, tenantId);
        int deploymentsSize = deployments.get("size").asInt();
        if (deploymentsSize == 0) {
            LOGGER.error("No deployment found name {}.", name);
            throw new RuntimeException("No deployment found " + name);
        }

        try {
            if (loginToApp(client)) {
                JsonNode data = deployments.get("data");
                for (JsonNode deployment : data) {
                    deleteDeployment(client, deployment.get("id").asText());
                }
            }
        } catch (URISyntaxException e) {
            LOGGER.error("Unable to save model to file", e);
            throw new RuntimeException(e);
        }
        return null;
    }

    protected JsonNode getDeployments(CloseableHttpClient client, String name, String tenantId) {
        URIBuilder uriBuilder;
        HttpGet httpGet;
        try {
            uriBuilder = new URIBuilder(configuration.getRestURL() + "app-api/app-repository/deployments").
                    addParameter("sort", "deployTime").
                    addParameter("order", "desc");
            if (!StringUtils.isEmpty(name)) {
                uriBuilder.addParameter("nameLike", name);
            }
            if (!StringUtils.isEmpty(tenantId)) {
                uriBuilder.addParameter("tenantId", tenantId);
            }
            httpGet = new HttpGet(uriBuilder.build());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        LOGGER.info("Calling flowable rest api {} to get deployments", httpGet.getURI().toString());
        CloseableHttpResponse response = executeBinaryRequest(client, httpGet, true);

        JsonNode responseNode = readContent(response);
        closeResponse(response);
        return responseNode;
    }

}
