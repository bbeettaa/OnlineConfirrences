package org.com;

import org.com.services.OnlineRoomWebSocketHandler;
import org.com.services.RoomManager;
import org.kurento.client.KurentoClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@SpringBootApplication
@SpringBootConfiguration
@EnableWebSocket
public class Main implements WebSocketConfigurer {
    private final Environment environment;

    public Main(Environment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @EventListener
    public void onApplicationEvent(WebServerInitializedEvent event) {
        String host = environment.getProperty("server.address", "localhost");
        String pp = environment.getProperty("server.address", "localhost");
        int port = event.getWebServer().getPort();
        System.out.println("Application started at http://" + host + ":" + port);
        System.out.println("Application started at https://" + host + ":" + port);
    }

    @Bean
    public ServletServerContainerFactoryBean createServletServerContainerFactoryBean() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(32768);
        return container;
    }

    @Bean
    public KurentoClient kurento() {
        return KurentoClient.create();
    }

    @Bean
    public OnlineRoomWebSocketHandler handler() {
        return new OnlineRoomWebSocketHandler();
    }

    @Bean
    public RoomManager roomManager(){
        return new RoomManager(kurento());
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler(), "/stream");
    }

}

