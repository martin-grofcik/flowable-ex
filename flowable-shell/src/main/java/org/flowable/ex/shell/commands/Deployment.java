package org.flowable.ex.shell.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHeaders;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.flowable.ex.shell.Configuration;
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
import java.nio.file.Paths;
import java.util.Collections;
import java.util.zip.ZipInputStream;

@ShellComponent
public class Deployment {
    private static final Logger LOGGER = LoggerFactory.getLogger(Deployment.class);

    @Autowired
    private Configuration configuration;
    @Autowired
    private ObjectMapper objectMapper;

    private CloseableHttpClient client;

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
        client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();

        // Upload a bar-file using multipart-data
        HttpPost httpPost = new HttpPost(configuration.getRestURL() + "app-repository/deployments");
        try (FileInputStream fis = new FileInputStream(pathToApplication);
             BufferedInputStream bis = new BufferedInputStream(fis);
             ZipInputStream zis = new ZipInputStream(bis)) {

            httpPost.setEntity(
                    HttpMultipartHelper.getMultiPartEntity(fileName, "application/zip", zis, Collections.singletonMap("tenantId", tenantId)));
            LOGGER.info("Calling flowable rest api {} to deploy {} into tenantId {}", httpPost.getURI().toString(), pathToApplication, tenantId);
            CloseableHttpResponse response = executeBinaryRequest(httpPost, false);

            JsonNode responseNode = readContent(response);
            closeResponse(response);
            return responseNode.toPrettyString();

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                LOGGER.error("Unable to close client", e);
            }
        }
    }

    protected CloseableHttpResponse executeBinaryRequest(HttpUriRequest request, boolean addJsonContentType) {
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
