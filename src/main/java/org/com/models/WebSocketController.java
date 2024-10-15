package org.com.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController("/api/socket")
@EnableWebSocket
public class WebSocketController implements WebSocketConfigurer {
    private final Map<String, Map<String, WebSocketSession>> rooms = new HashMap<>();
    private final ObjectMapper om = new ObjectMapper();



    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Регистрируем обработчик для WebSocket по адресу /ws/{roomId}
        registry.addHandler(new WebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                String roomId = (String) session.getAttributes().get("roomId");
//                sessions.put(roomId, session);
                System.out.println("Session established for room: " + roomId);
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
                MessageRequest payload = new ObjectMapper().readValue(message.getPayload().toString(), MessageRequest.class);
                System.out.println("Получено сообщение: " + payload);

                if (payload.getMessageStatus().equals(MessageStatus.ESTABLISHING)) {
                    session.getAttributes().put("roomId", payload.getRoomId());
                    rooms.computeIfAbsent(payload.getRoomId(), k -> new HashMap<>());
                    rooms.get(payload.getRoomId()).put(payload.getUserName(), session);

                    notifyParticipants(payload.getRoomId());
                    System.out.println("Пользователь: " + payload.getUserName() + " присоединился к комнате: " + payload.getRoomId());
                    // Ответить клиенту
                    session.sendMessage(new TextMessage(om.writeValueAsString(new MessageRequest(payload.getUserName(),payload.getRoomId(), "Привет " + payload.getUserName() + " от сервера! Ты в комнате. " + payload.getRoomId(), MessageStatus.MESSAGE))));


                } else if (payload.getMessageStatus().equals(MessageStatus.MESSAGE)) {
                    for (WebSocketSession s : rooms.get(payload.getRoomId()).values()) {
                        if (payload.getRoomId().equals(s.getAttributes().get("roomId"))) {
                            s.sendMessage(new TextMessage(om.writeValueAsString(new MessageRequest(payload.getUserName(),payload.getRoomId(), payload.getUserName() + ": " + payload.getMessage(), MessageStatus.MESSAGE))));
                        }
                    }
                }
            }

            private void notifyParticipants(String roomId) throws Exception {
                List<String> participantNames = new ArrayList<>(rooms.get(roomId).keySet());

                // Создаем сообщение с новым статусом
                MessageRequest participantsMessage = new MessageRequest();
                participantsMessage.setMessageStatus(MessageStatus.PARTICIPANTS);
                participantsMessage.setRoomId(roomId);
                participantsMessage.setMessage(om.writeValueAsString(participantNames));

                // Отправляем сообщение всем пользователям в комнате
                for (WebSocketSession s : rooms.get(roomId).values()) {
                    if (s.isOpen()) {
                        s.sendMessage(new TextMessage(om.writeValueAsString(participantsMessage)));
                    }
                }
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                System.out.println(exception.getMessage());
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                String roomId = (String) session.getAttributes().get("roomId");
                if (roomId != null) {
                    rooms.get(roomId).values().remove(session);
                    System.out.println("Session closed for room: " + roomId);
                    notifyParticipants(roomId);
                }
            }

            @Override
            public boolean supportsPartialMessages() {
                return false;
            }
        }, "/ws/{roomId}").setAllowedOrigins("*");

    }


    @PostMapping("/join")
    public void joinRoom(@RequestBody MessageRequest messageRequest) {
        System.out.println(messageRequest.getUserName() + " joined the room: " + messageRequest.getRoomId());
    }

    @PostMapping("/remove")
    public void leaveRoom(@RequestBody MessageRequest messageRequest) {
        System.out.println(messageRequest.getUserName() + " left the room: " + messageRequest.getRoomId());
    }


}
