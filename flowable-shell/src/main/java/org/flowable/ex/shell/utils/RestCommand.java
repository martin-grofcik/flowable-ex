package org.flowable.ex.shell.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHeaders;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.util.Collections;

import static java.net.HttpURLConnection.HTTP_OK;

public class RestCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestCommand.class);

    @Autowired
    protected Configuration configuration;
    @Autowired
    protected ObjectMapper objectMapper;

    protected CloseableHttpClient createClient() {
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(configuration.getLogin(), configuration.getPassword());
        provider.setCredentials(AuthScope.ANY, credentials);
        return HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
    }

    protected JsonNode uploadFile(CloseableHttpClient client, String pathToApplication, String file, String fileName, String tenantId, HttpPost httpPost) {
        try (FileInputStream fis = new FileInputStream(pathToApplication);
             BufferedInputStream bis = new BufferedInputStream(fis)) {

            httpPost.setEntity(
                    HttpMultipartHelper.getMultiPartEntity(file, fileName, "application/zip", bis, Collections.singletonMap("tenantId", tenantId)));
            LOGGER.info("Calling flowable rest api {} to deploy {} into tenantId {}", httpPost.getURI().toString(), pathToApplication, tenantId);
            CloseableHttpResponse response = executeBinaryRequest(client, httpPost, false);

            JsonNode responseNode = readContent(response);
            closeResponse(response);
            return responseNode;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected boolean loginToApp(CloseableHttpClient client) throws URISyntaxException {
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

    protected JsonNode executeWithClient(ExecuteWithClient exec) {
        CloseableHttpClient client = createClient();
        try {
            return exec.execute(client);
        } finally {
            closeClient(client);
        }
    }

    protected void closeClient(CloseableHttpClient client) {
        try {
            client.close();
        } catch (IOException e) {
            LOGGER.error("Unable to close client", e);
        }
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

    protected void closeResponse(CloseableHttpResponse response) {
        if (response != null) {
            try {
                response.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
