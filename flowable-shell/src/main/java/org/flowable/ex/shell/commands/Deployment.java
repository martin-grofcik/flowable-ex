package org.flowable.ex.shell.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.flowable.ex.shell.utils.Configuration;
import org.flowable.ex.shell.utils.HttpMultipartHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.util.StringUtils;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.zip.ZipInputStream;

import static java.net.HttpURLConnection.HTTP_OK;

@ShellComponent
public class Deployment {
    private static final Logger LOGGER = LoggerFactory.getLogger(Deployment.class);

    private static final Map<String, String> MODEL_TYPES = Collections.unmodifiableMap(Map.of(
            "bpmn", "0",
            "form", "2",
            "app", "3",
            "decision-table", "4",
            "cmmn", "5",
            "decision-service", "6"
    ));

    @Autowired
    private Configuration configuration;
    @Autowired
    private ObjectMapper objectMapper;

    @ShellMethod("Configure flowable rest endpoint")
    public String configure(@ShellOption(defaultValue = "") String login,
                            @ShellOption(defaultValue = "") String password,
                            @ShellOption(defaultValue = "") String restUrl) {
        if (!StringUtils.isEmpty(login)) configuration.setLogin(login);
        if (!StringUtils.isEmpty(password)) configuration.setPassword(password);
        if (!StringUtils.isEmpty(restUrl)) {
            if (!restUrl.endsWith("/")) {
                restUrl += "/";
            }
            configuration.setRestURL(restUrl);
        }

        LOGGER.info("Current configuration restUrl {}, login {}", restUrl, login);
        return configuration.getLogin() + "@" + configuration.getRestURL();
    }

    @ShellMethod("Deploy given application")
    public JsonNode deploy(String pathToApplication,
                           @ShellOption(defaultValue = "") String deploymentName,
                           @ShellOption(defaultValue = "") String tenantId) {
        String mandatoryFileName = StringUtils.isEmpty(deploymentName) ? Paths.get(pathToApplication).getFileName().toString() : deploymentName;

        return executeWithClient(client -> {
            HttpPost httpPost = new HttpPost(configuration.getRestURL() + "app-api/app-repository/deployments");
            return uploadFile(client, pathToApplication, mandatoryFileName, tenantId, httpPost);
        });
    }

    @ShellMethod("Export model from modeler to file")
    public void export(@ShellOption(defaultValue = "app") String type,
                       @ShellOption String name,
                       @ShellOption(defaultValue = "") String tenantId,
                       @ShellOption String outputFileName) {
        executeWithModelId(type, name, (client, modelId) -> saveModelToFile(client, modelId, outputFileName));
    }

    @ShellMethod(value = "Delete model from modeler", key = {"rm", "delete-model"})
    public void deleteModel(String name,
                       @ShellOption(defaultValue = "app") String type,
                       @ShellOption(defaultValue = "") String tenantId) {
        executeWithModelId(type, name, this::deleteModel);
    }

    @ShellMethod(value = "Delete all deployments with given name, tenantId from runtime. WARNING - use only for testing purposes",
    key ={"rmd", "delete-deployments"})
    public void deleteDeployments(String name, @ShellOption(defaultValue = "") String tenantId) {
        executeWithClient(client -> deleteDeployments(client, name, tenantId));
    }

    @ShellMethod(value="Import file to modeler", key="import")
    public void importToModeler(String inputFileName,
                                @ShellOption(defaultValue = "") String tenantId) {
        executeWithClient(client -> importApp(client, inputFileName, Paths.get(inputFileName).getFileName().toString(), tenantId));
    }

    @ShellMethod(value = "list models", key={"ls", "list"})
    public JsonNode list(@ShellOption(defaultValue = "") String name, @ShellOption(defaultValue = "app") String type) {
        return executeWithClient(client -> getModels(client, type, name));
    }

    @ShellMethod(value = "list deployments", key = {"list-deployments", "lsd"})
    public JsonNode listDeployments(@ShellOption(defaultValue = "") String name, @ShellOption(defaultValue = "") String tenantId) {
        return executeWithClient(client -> getDeployments(client, name, tenantId));
    }

    protected CloseableHttpClient createClient() {
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(configuration.getLogin(), configuration.getPassword());
        provider.setCredentials(AuthScope.ANY, credentials);
        return HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
    }

    protected JsonNode uploadFile(CloseableHttpClient client, String pathToApplication, String fileName, String tenantId, HttpPost httpPost) {
        try (FileInputStream fis = new FileInputStream(pathToApplication);
             BufferedInputStream bis = new BufferedInputStream(fis);
             ZipInputStream zis = new ZipInputStream(bis)) {

            httpPost.setEntity(
                    HttpMultipartHelper.getMultiPartEntity(fileName, "application/zip", zis, Collections.singletonMap("tenantId", tenantId)));
            LOGGER.info("Calling flowable rest api {} to deploy {} into tenantId {}", httpPost.getURI().toString(), pathToApplication, tenantId);
            CloseableHttpResponse response = executeBinaryRequest(client, httpPost, false);

            JsonNode responseNode = readContent(response);
            closeResponse(response);
            return responseNode;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected void executeWithModelId(String type, String name, ExecuteWithModelId exec) {
        executeWithClient(client -> {
            JsonNode responseNode = getModels(client, type, name);
            int modelsSize = responseNode.get("size").asInt();
            if (modelsSize > 1) {
                LOGGER.error("Ambiguous model name {} of type {}.", name, type);
                throw new RuntimeException("More than one model " + name + "returned [" + modelsSize + "]");
            }
            if (modelsSize == 0) {
                LOGGER.error("No model found name {} of type {}.", name, type);
                throw new RuntimeException("No model found " + name);
            }

            String modelId = responseNode.get("data").get(0).get("id").asText();
            try {
                if (loginToApp(client)) {
                    exec.execute(client, modelId);
                }
            } catch (URISyntaxException e) {
                LOGGER.error("Unable to save model to file", e);
                throw new RuntimeException(e);
            }
            return null;
        });
    }

    protected JsonNode executeWithClient(ExecuteWithClient exec) {
        CloseableHttpClient client = createClient();
        try {
            return exec.execute(client);
        } finally {
            closeClient(client);
        }
    }

    protected void saveModelToFile(CloseableHttpClient client, String modelId, String outputFileName) {
        try {
            URIBuilder uriBuilder = new URIBuilder(configuration.getRestURL() + "modeler-app/rest/app-definitions/" + modelId + "/export");
            HttpGet httpGet = new HttpGet(uriBuilder.build());
            LOGGER.info("Getting model id {} to file {}.", modelId, outputFileName);
            CloseableHttpResponse response = executeBinaryRequest(client, httpGet, false);
            try {
                InputStream content = response.getEntity().getContent();
                FileUtils.copyInputStreamToFile(content, new File(outputFileName));
            } finally {
                closeResponse(response);
            }
        } catch (IOException | URISyntaxException e) {
            LOGGER.error("Unable to save file.", e);
        }
    }

    protected void deleteModel(CloseableHttpClient client, String modelId){
        try {
            URIBuilder uriBuilder = new URIBuilder(configuration.getRestURL() + "modeler-app/rest/models/" + modelId + "/export");
            HttpDelete httpDelete = new HttpDelete(uriBuilder.build());
            LOGGER.info("Deleting model id {}.", modelId);
            CloseableHttpResponse response = executeBinaryRequest(client, httpDelete, false);
            try {
                LOGGER.info("Delete response {}", response.getStatusLine());
            } finally {
                closeResponse(response);
            }
        } catch (URISyntaxException e) {
            LOGGER.error("Unable to deleteModel.", e);
        }
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

    protected JsonNode importApp(CloseableHttpClient client, String pathToFile, String fileName, String tenantId){
        try {
            URIBuilder uriBuilder = new URIBuilder(configuration.getRestURL() + "modeler-app/rest/app-definitions/import");
            HttpPost httpPost = new HttpPost(uriBuilder.build());
            loginToApp(client);
            return uploadFile(client, pathToFile, fileName, tenantId, httpPost);
        } catch (URISyntaxException e) {
            LOGGER.error("Unable to import model.", e);
        }
        return null;
    }

    private boolean loginToApp(CloseableHttpClient client) throws URISyntaxException {
        URIBuilder idmUriBuilder = new URIBuilder(configuration.getRestURL() + "app/authentication").
                addParameter("j_username", "admin").addParameter("j_password", "test").
                addParameter("_spring_security_remember_me", "true").addParameter("submit", "Login");
        HttpPost appLogin = new HttpPost(idmUriBuilder.build());
        CloseableHttpResponse idmResponse = executeBinaryRequest(client, appLogin, false);
        try {
            if (idmResponse.getStatusLine().getStatusCode() != HTTP_OK) {
                LOGGER.error("Unable to establish connection to modeler app {}", idmResponse.getStatusLine());
                return false;
            }
        } finally {
            closeResponse(idmResponse);
        }
        return true;
    }

    protected JsonNode getModels(CloseableHttpClient client, String type, String name) {
        URIBuilder uriBuilder;
        HttpGet httpGet;
        try {
            uriBuilder = new URIBuilder(configuration.getRestURL() + "api/editor/models").
                    addParameter("modelType", getModelType(type)).
                    addParameter("filterText", name).
                    addParameter("sort", "modifiedDesc");
            httpGet = new HttpGet(uriBuilder.build());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        LOGGER.info("Calling flowable rest api {} to get models", httpGet.getURI().toString());
        CloseableHttpResponse response = executeBinaryRequest(client, httpGet, true);

        JsonNode responseNode = readContent(response);
        closeResponse(response);
        return responseNode;
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

    protected void closeClient(CloseableHttpClient client) {
        try {
            client.close();
        } catch (IOException e) {
            LOGGER.error("Unable to close client", e);
        }
    }

    protected String getModelType(String type) {
        if (!MODEL_TYPES.containsKey(type)) {
            throw new IllegalArgumentException("Parameter type " + type + " is not supported. Valid parameter types are " + MODEL_TYPES.keySet() + ".");
        }
        return MODEL_TYPES.get(type);
    }

    protected CloseableHttpResponse executeBinaryRequest(CloseableHttpClient client, HttpUriRequest request, boolean addJsonContentType) {
        try {
            if (addJsonContentType && request.getFirstHeader(HttpHeaders.CONTENT_TYPE) == null) {
                // Revert to default content-type
                request.addHeader(new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"));
            }
            return client.execute(request);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected JsonNode readContent(CloseableHttpResponse response) {
        try {
            return objectMapper.readTree(response.getEntity().getContent());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void closeResponse(CloseableHttpResponse response) {
        if (response != null) {
            try {
                response.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @FunctionalInterface
    interface ExecuteWithModelId {
        void execute(CloseableHttpClient client, String modelId);
    }

    @FunctionalInterface
    interface ExecuteWithClient {
        JsonNode execute(CloseableHttpClient client);
    }
}
