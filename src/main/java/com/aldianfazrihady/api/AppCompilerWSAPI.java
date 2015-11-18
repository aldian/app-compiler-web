package com.aldianfazrihady.api;

import org.apache.http.HttpEntity;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

/**
 * Created by AldianFazrihady on 11/14/15.
 */
public class AppCompilerWSAPI {
    private static final String proto = "http";
    private static final String colon_slashes = "://";
    private static final String colon = ":";
    private static final String slash_ws_slash = "/ws/";

    private String host;
    private int port;
    private String accessToken;

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public AppCompilerWSAPI(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String login(String username, String password) throws IOException {
        Content content = Request.Post(proto + colon_slashes + host + colon + port + slash_ws_slash + "login")
            .bodyForm(Form.form().add("username", username).add("password", password).build())
            .execute().returnContent();
        accessToken = content.asString();
        return accessToken;
    }

    public void logout() throws IOException {
        Request.Post(proto + colon_slashes + host + colon + port + slash_ws_slash + "logout")
            .bodyForm(Form.form().add("accessToken", accessToken).build())
            .execute();
    }

    public String compile(byte[] zipData) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(proto + colon_slashes + host + colon + port + slash_ws_slash + "compile");
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addTextBody("accessToken", accessToken, ContentType.TEXT_PLAIN);
        builder.addBinaryBody("zipFile", zipData, ContentType.APPLICATION_OCTET_STREAM, UUID.randomUUID().toString() + ".zip");
        HttpEntity multipart = builder.build();
        httpPost.setEntity(multipart);
        CloseableHttpResponse response = httpClient.execute(httpPost);
        HttpEntity responseEntity = response.getEntity();
        StringBuilder strBuilder = new StringBuilder();
        char[] cbuf = new char[1024];
        InputStreamReader reader = new InputStreamReader(responseEntity.getContent());
        for (int off = 0, nRead = 0; (nRead = reader.read(cbuf, 0, cbuf.length)) > 0; off += nRead) {
            strBuilder.append(cbuf, 0, nRead);
        }
        httpClient.close();
        response.close();
        return strBuilder.toString();
    }
}
