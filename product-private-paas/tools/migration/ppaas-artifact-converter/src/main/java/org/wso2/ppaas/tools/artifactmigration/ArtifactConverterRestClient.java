/*
 * Copyright (c) 2005-2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.ppaas.tools.artifactmigration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hazelcast.client.connection.ClientConnectionManager;
import org.apache.abdera.i18n.iri.Scheme;
import org.apache.abdera.i18n.iri.SchemeRegistry;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;

import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;

import javax.net.ssl.SSLContext;
import org.apache.http.conn.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class ArtifactConverterRestClient {
    public static final int SO_TIMEOUT = 120000;
    public static final int CONNECT_TIMEOUT = 120000;
    public static final int CONNECTION_TIMEOUT = 120000;
    public static final int MAX_TOTAL_CONNECTIONS = 100;
    public static final int MAX_ROUTE_CONNECTIONS = 100;
    private static final Log log = LogFactory.getLog(ArtifactConverterRestClient.class);
    private HttpClient httpClient;
    private String username;
    private String password;
    private HttpResponseHandler responseHandler;
    private GsonBuilder gsonBuilder = new GsonBuilder();
    private Gson gson = gsonBuilder.create();

    public ArtifactConverterRestClient(String username, String password) throws Exception {

//        System.setProperty("javax.net.ssl.trustStore",
//                getResourcesFolderPath() + File.separator + "client-truststore.jks");
//        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
//        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("javax.net.ssl.trustStore",
                getResourcesFolderPath() + File.separator + "wso2carbon.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");


        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(MAX_TOTAL_CONNECTIONS);
        cm.setDefaultMaxPerRoute(MAX_ROUTE_CONNECTIONS);
        RequestConfig requestConfig = RequestConfig.copy(RequestConfig.DEFAULT).setSocketTimeout(SO_TIMEOUT)
                .setConnectTimeout(CONNECT_TIMEOUT).setConnectionRequestTimeout(CONNECTION_TIMEOUT).build();

        SSLContextBuilder builder = new SSLContextBuilder();
        SSLConnectionSocketFactory sslConnectionFactory;
        builder.loadTrustMaterial(null, new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                return true;
            }
        });
        sslConnectionFactory = new SSLConnectionSocketFactory(builder.build());
        this.httpClient = HttpClients.custom().setSSLSocketFactory(sslConnectionFactory).setConnectionManager(cm)
                .create().setDefaultRequestConfig(requestConfig).build();

        this.username = username;
        this.password = password;
        this.responseHandler = new HttpResponseHandler();

//        SSLContext sslContext = SSLContext.getInstance("SSL");
//
//        // set up a TrustManager that trusts everything
//        sslContext.init(null, new TrustManager[] { new X509TrustManager() {
//            public X509Certificate[] getAcceptedIssuers() {
//                System.out.println("getAcceptedIssuers =============");
//                return null;
//            }
//
//            public void checkClientTrusted(X509Certificate[] certs,
//                    String authType) {
//                System.out.println("checkClientTrusted =============");
//            }
//
//            public void checkServerTrusted(X509Certificate[] certs,
//                    String authType) {
//                System.out.println("checkServerTrusted =============");
//            }
//        } }, new SecureRandom());
//
//        SSLSocketFactory sf = new SSLSocketFactory(sslContext);
//        Scheme httpsScheme = new Scheme("https", 443, sf);
//        SchemeRegistry schemeRegistry = new SchemeRegistry();
//        schemeRegistry.register(httpsScheme);
//
//        // apache HttpClient version >4.2 should use BasicClientConnectionManager
//        ClientConnectionManager cm = new SingleClientConnManager(schemeRegistry);
//        HttpClient httpClient = new DefaultHttpClient(cm);
    }

    private static String getResourcesFolderPath() {
        //String path = ArtifactConverterRestClient.class.getResource("/").getPath();
        String path = System.getProperty("user.dir")+ File.separator+ ".." + File.separator+ "resources";
        log.info("Resource folder path: " + path);
        return StringUtils.removeEnd(path, File.separator);
    }

    /**
     * Get the username and password
     *
     * @return username:password
     */
    private String getUsernamePassword() {
        return this.username + ":" + this.password;
    }

    /**
     * Handle http post request. Return String
     *
     * @param resourcePath    This should be REST endpoint
     * @param jsonParamString The json string which should be executed from the post request
     * @return The HttpResponse
     * @throws Exception if any errors occur when executing the request
     */
    public HttpResponse doPost(URI resourcePath, String jsonParamString) throws IOException {
        HttpPost postRequest = null;
        try {
            postRequest = new HttpPost(resourcePath);
            StringEntity input = new StringEntity(jsonParamString);
            input.setContentType("application/json");
            postRequest.setEntity(input);

            String userPass = getUsernamePassword();
            String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter
                    .printBase64Binary(userPass.getBytes("UTF-8"));
            postRequest.addHeader("Authorization", basicAuth);

            return httpClient.execute(postRequest, responseHandler);
        } finally {
            releaseConnection(postRequest);
        }
    }

    /**
     * Handle http get request. Return String
     *
     * @param resourcePath This should be REST endpoint
     * @return The HttpResponse
     * @throws org.apache.http.client.ClientProtocolException and IOException
     *                                                        if any errors occur when executing the request
     */
    public HttpResponse doGet(URI resourcePath) throws IOException {
        HttpGet getRequest = null;
        try {
            getRequest = new HttpGet(resourcePath);
            getRequest.addHeader("Content-Type", "application/json");
            String userPass = getUsernamePassword();
            String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter
                    .printBase64Binary(userPass.getBytes("UTF-8"));
            getRequest.addHeader("Authorization", basicAuth);
            return httpClient.execute(getRequest, responseHandler);
        } finally {
            releaseConnection(getRequest);
        }
    }

    public HttpResponse doDelete(URI resourcePath) throws IOException {
        HttpDelete httpDelete = null;
        try {
            httpDelete = new HttpDelete(resourcePath);
            httpDelete.addHeader("Content-Type", "application/json");
            String userPass = getUsernamePassword();
            String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter
                    .printBase64Binary(userPass.getBytes("UTF-8"));
            httpDelete.addHeader("Authorization", basicAuth);
            return httpClient.execute(httpDelete, responseHandler);
        } finally {
            releaseConnection(httpDelete);
        }
    }

    public HttpResponse doPut(URI resourcePath, String jsonParamString) throws IOException {
        HttpPut putRequest = null;
        try {
            putRequest = new HttpPut(resourcePath);
            StringEntity input = new StringEntity(jsonParamString);
            input.setContentType("application/json");
            putRequest.setEntity(input);
            String userPass = getUsernamePassword();
            String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter
                    .printBase64Binary(userPass.getBytes("UTF-8"));
            putRequest.addHeader("Authorization", basicAuth);
            return httpClient.execute(putRequest, responseHandler);
        } finally {
            releaseConnection(putRequest);
        }
    }

    private void releaseConnection(HttpRequestBase request) {
        if (request != null) {
            request.releaseConnection();
        }
    }
}
