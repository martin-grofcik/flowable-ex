package org.flowable.ex.shell.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHeaders;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.zip.ZipInputStream;

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
    )    );

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
    public String deploy(String pathToApplication,
                         @ShellOption(defaultValue = "") String fileName,
                         @ShellOption(defaultValue = "") String tenantId) {
        if (StringUtils.isEmpty(fileName)) {
            fileName = Paths.get(pathToApplication).getFileName().toString();
        }
        // Create Http client
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(configuration.getLogin(), configuration.getPassword());
        provider.setCredentials(AuthScope.ANY, credentials);
        CloseableHttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();

        // Upload a bar-file using multipart-data
        HttpPost httpPost = new HttpPost(configuration.getRestURL() + "app-api/app-repository/deployments");
        try (FileInputStream fis = new FileInputStream(pathToApplication);
             BufferedInputStream bis = new BufferedInputStream(fis);
             ZipInputStream zis = new ZipInputStream(bis)) {

            httpPost.setEntity(
                    HttpMultipartHelper.getMultiPartEntity(fileName, "application/zip", zis, Collections.singletonMap("tenantId", tenantId)));
            LOGGER.info("Calling flowable rest api {} to deploy {} into tenantId {}", httpPost.getURI().toString(), pathToApplication, tenantId);
            CloseableHttpResponse response = executeBinaryRequest(client, httpPost, false);

            JsonNode responseNode = readContent(response);
            closeResponse(response);
            return responseNode.toPrettyString();

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            closeClient(client);
        }
    }

    @ShellMethod("Export model from modeler to file")
    public String export(@ShellOption(defaultValue = "app") String type,
                         @ShellOption(defaultValue = "") String name,
                         @ShellOption(defaultValue = "") String tenantId) throws URISyntaxException {
        // Create Http client
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(configuration.getLogin(), configuration.getPassword());
        provider.setCredentials(AuthScope.ANY, credentials);
        CloseableHttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();

        try {
            URIBuilder uriBuilder = new URIBuilder(configuration.getRestURL() + "api/editor/models").
                    addParameter("modelType", getModelType(type)).
                    addParameter("filterText", name).
                    addParameter("sort", "modifiedDesc");

            HttpGet httpGet = new HttpGet(uriBuilder.build());
            LOGGER.info("Calling flowable rest api {} to get models", httpGet.getURI().toString());
            CloseableHttpResponse response = executeBinaryRequest(client, httpGet, true);

            JsonNode responseNode = readContent(response);
            closeResponse(response);

            return responseNode.toPrettyString();
        } finally {
            closeClient(client);
        }
    }

    private void closeClient(CloseableHttpClient client) {
        try {
            client.close();
        } catch (IOException e) {
            LOGGER.error("Unable to close client", e);
        }
    }

    private String getModelType(String type) {
        if (!MODEL_TYPES.containsKey(type)) {
            throw new IllegalArgumentException("Parameter type " + type + " is not supported. Valid parameter types are " + MODEL_TYPES.keySet() + ".");
        }
        return MODEL_TYPES.get(type);
    }

    protected CloseableHttpResponse executeBinaryRequest(CloseableHttpClient client, HttpUriRequest request, boolean addJsonContentType) {
        CloseableHttpResponse response;
        try {
            if (addJsonContentType && request.getFirstHeader(HttpHeaders.CONTENT_TYPE) == null) {
                // Revert to default content-type
                request.addHeader(new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"));
            }
            response = client.execute(request);


            int responseStatusCode = response.getStatusLine().getStatusCode();

            return response;

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public JsonNode readContent(CloseableHttpResponse response) {
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

}
