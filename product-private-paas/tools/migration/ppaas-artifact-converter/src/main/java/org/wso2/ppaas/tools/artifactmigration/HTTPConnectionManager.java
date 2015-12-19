package org.wso2.ppaas.tools.artifactmigration;

import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public class HTTPConnectionManager {
    private static final int MAX_TOTAL_CONNECTIONS = 100;
    private static final int DEFAULT_MAX_PER_ROUTE = 20;
    private HTTPConnectionManager() {
    }
    public static HTTPConnectionManager getInstance() {
        return InstanceHolder.instance;
    }
    public HttpClientConnectionManager getHttpConnectionManager() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        // TODO: Introduce configurable variable for Max total and max per router variables.
        cm.setMaxTotal(MAX_TOTAL_CONNECTIONS);
        cm.setDefaultMaxPerRoute(DEFAULT_MAX_PER_ROUTE);
        //HttpHost localhost = new HttpHost("localhost", 80);
        //cm.setMaxPerRoute(new HttpRoute(localhost), 50);
        return cm;
    }
    private static class InstanceHolder {
        public static HTTPConnectionManager instance = new HTTPConnectionManager();
    }
}
