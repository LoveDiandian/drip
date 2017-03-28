package com.drip.http;

import com.google.gson.Gson;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;

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

    //第二次提交

    //第三次提交

    class IdleConnectionMonitorThread extends Thread {
        private final PoolingHttpClientConnectionManager connMgr;
        private volatile boolean                         shutdown;

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
