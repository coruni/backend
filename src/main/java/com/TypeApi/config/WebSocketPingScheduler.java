package com.TypeApi.config;
import javax.websocket.Session;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WebSocketPingScheduler {

    private static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static void startPingScheduler(Session session) {
        if (scheduler.isTerminated()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
        }

        Runnable pingTask = () -> {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText("PING");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                stopPingScheduler();
            }
        };

        scheduler.scheduleAtFixedRate(pingTask, 0, 30, TimeUnit.SECONDS);
    }

    public static void stopPingScheduler() {
        scheduler.shutdown();
    }
}