package com.drip.http;

import com.google.gson.Gson;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HTTP;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by zhouguangsheng on 2017/3/20.
 */
public class HttpClientUtil {

    private int socket_timeout = 8000;
    private int connect_timeout = 8000;
    private int connect_request_timeout = 2000;
    private int connect_max_total = 800;
    private int connect_max_per_route = 500;
    private PoolingHttpClientConnectionManager poolConnManager = null;
    private IdleConnectionMonitorThread idleConnectionMonitorThread = null;
    private Gson gson = new Gson();

    private Logger log = LoggerFactory.getLogger(HttpClientUtil.class);

    private HttpClientUtil() {
        init();
    }

    private static class SingletonContainer {
        private static HttpClientUtil _hinstance = new HttpClientUtil();
    }

    /*单例写法*/
    public HttpClientUtil getInstance() {
        return SingletonContainer._hinstance;
    }

    private void init() {
        try {
            ConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();
            SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(), NoopHostnameVerifier.INSTANCE);
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create().register("http", plainsf)
                    .register("https", sslsf).build();
            poolConnManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            // Increase max total connection to 200
            poolConnManager.setMaxTotal(connect_max_total);
            // Increase default max connection per route to 20
            poolConnManager.setDefaultMaxPerRoute(connect_max_per_route);
            SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(socket_timeout).build();
            poolConnManager.setDefaultSocketConfig(socketConfig);

            // 定期清理无效、空闲连接
            idleConnectionMonitorThread = new IdleConnectionMonitorThread(poolConnManager);
            idleConnectionMonitorThread.start();
            // 增加关闭钩子
            addHook();
        } catch (Exception e) {
            log.warn("NetUtil init Exception", e);
        }
    }

    private void addHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                log.info("shutdown hook get");
                close();
                log.info("shutdown hook end");
            }
        }));
    }

    private void close() {
        if (poolConnManager != null) {
            poolConnManager.close();
        }
        if (idleConnectionMonitorThread != null) {
            idleConnectionMonitorThread.shutdown();
        }
    }

    class IdleConnectionMonitorThread extends Thread {
        private final PoolingHttpClientConnectionManager connMgr;
        private volatile boolean shutdown;

        public IdleConnectionMonitorThread(PoolingHttpClientConnectionManager connMgr) {
            super();
            this.setName("idle-connection-monitor");
            this.setDaemon(true);
            this.connMgr = connMgr;
        }

        @Override
        public void run() {
            try {
                while (!shutdown) {
                    synchronized (this) {
                        wait(5000);
                        // Close expired connections
                        connMgr.closeExpiredConnections();
                        // Optionally, close connections
                        // that have been idle longer than 30 sec
                        connMgr.closeIdleConnections(30, TimeUnit.SECONDS);
                    }
                }
            } catch (InterruptedException ex) {
                // terminate
            }
        }

        public void shutdown() {
            synchronized (this) {
                shutdown = true;
                notifyAll();
            }
        }
    }
}
