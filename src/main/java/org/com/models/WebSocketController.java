package org.com.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.com.models.api.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.*;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.io.IOException;
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
            public void afterConnectionEstablished(WebSocketSession session)   {
                String roomId = (String) session.getAttributes().get("roomId");
                System.out.println("Session established for room: " + roomId);
            }

            @Override
            public void handleMessage(WebSocketSession session, WebSocketMessage<?> message)   {
                try {
                    MessageRequest payload = om.readValue(message.getPayload().toString(), MessageRequest.class);
                    System.out.println("Получено сообщение: " + payload);

                    if (payload.getMessageStatus().equals(MessageStatus.ESTABLISHING)) {
                        handleEstablishing(session, payload);
                    } else if (payload.getMessageStatus().equals(MessageStatus.MESSAGE)) {
                        handleMessage(payload);
                    } else if (payload.getMessageStatus().equals(MessageStatus.ICE_CANDIDATE)) {
                        handleIceCandidate(session, payload);
                    } else if (payload.getMessageStatus().equals(MessageStatus.VIDEO_OFFER)){
                        handleVidioOffer(session, payload);
                    }else if (payload.getMessageStatus().equals(MessageStatus.VIDEO_ANSWER)) {
                        handleVideoAnswer(session, payload);
                    }
                } catch (Exception e){
                    System.out.println(e.getMessage());
                }
            }

            private void handleVideoAnswer(WebSocketSession session, MessageRequest payload) throws IOException {
                // Обработка видео-ответа
                System.out.println("Video answer received");
                SdpMessage videoAnswer = om.readValue(payload.getMessage().toString(), SdpMessage.class);
                String roomId = (String) session.getAttributes().get("roomId");

                // Переслать видео-ответ инициатору
                for (WebSocketSession s : rooms.get(roomId).values()) {
                    if (!s.equals(session)) {
                        s.sendMessage(new TextMessage(om.writeValueAsString(new MessageResponse(
                                om.writeValueAsString(videoAnswer), MessageStatus.VIDEO_ANSWER))));
                    }
                }
            }

            private void handleVidioOffer(WebSocketSession session, MessageRequest payload) throws IOException {
                System.out.println(MessageStatus.VIDEO_OFFER);
                // Логика обработки видео предложения
                System.out.println("Получен VIDEO_OFFER");

                // Десериализация SDP предложения из сообщения
                VideoOffer videoOffer = om.readValue(payload.getMessage().toString(), VideoOffer.class);

                // Отправка SDP предложения всем участникам комнаты (кроме отправителя)
                String roomId = session.getAttributes().get("roomId").toString();
                for (WebSocketSession s : rooms.get(roomId).values()) {
                    if (!s.equals(session)) { // Не отправляем предложение обратно отправителю
                        s.sendMessage(new TextMessage(om.writeValueAsString(
                                new MessageResponse(om.writeValueAsString(videoOffer), MessageStatus.VIDEO_OFFER))));
                    }
                }
            }

            private void handleIceCandidate(WebSocketSession session, MessageRequest payload) throws IOException {
                System.out.println("Получен ICE-кандидат: " + payload.getMessage());
                String roomId = (String) session.getAttributes().get("roomId");

                // Пересылка ICE-кандидатов всем остальным участникам в комнате
                System.out.println("ICE Candidate received");
                IceCandidateMessage iceCandidateMessage = om.readValue(payload.getMessage().toString(), IceCandidateMessage.class);

                // Переслать ICE-кандидата другим участникам комнаты
                for (WebSocketSession s : rooms.get(roomId).values()) {
                    if (!s.equals(session)) {
                        s.sendMessage(new TextMessage(om.writeValueAsString(new MessageResponse(
                                om.writeValueAsString(iceCandidateMessage), MessageStatus.ICE_CANDIDATE))));
                    }
                }
            }

            private void handleMessage(MessageRequest payload) throws IOException {
                UserMessage messageRequest = om.readValue(payload.getMessage().toString(), UserMessage.class);
                for (WebSocketSession s : rooms.get(messageRequest.getRoomId()).values()) {
                    if (messageRequest.getRoomId().equals(s.getAttributes().get("roomId"))) {
                        s.sendMessage(new TextMessage(om.writeValueAsString(new MessageResponse(om.writeValueAsString(
                                new UserMessage(
                                        messageRequest.getUserName(),
                                        messageRequest.getRoomId(),
                                        messageRequest.getUserName() + ": " + messageRequest.getMessage())
                        ),MessageStatus.MESSAGE))));
                    }
                }
            }

            private void handleEstablishing(WebSocketSession session, MessageRequest payload) throws Exception {
                UserMessage messageRequest = om.readValue(payload.getMessage().toString(), UserMessage.class);
                session.getAttributes().put("roomId", messageRequest.getRoomId());
                rooms.computeIfAbsent(messageRequest.getRoomId(), k -> new HashMap<>());
                rooms.get(messageRequest.getRoomId()).put(messageRequest.getUserName(), session);

                notifyParticipants(messageRequest.getRoomId());
                System.out.println("Пользователь: " + messageRequest.getUserName() + " присоединился к комнате: " + messageRequest.getRoomId());
                // Ответить клиенту
                session.sendMessage(new TextMessage(om.writeValueAsString(new MessageResponse(om.writeValueAsString(new UserMessage(
                                messageRequest.getUserName(),
                                messageRequest.getRoomId(),
                                "Привет " + messageRequest.getUserName() + " от сервера! Ты в комнате. " + messageRequest.getRoomId())
                ), MessageStatus.MESSAGE))));
            }

            private void notifyParticipants(String roomId) throws Exception {
                List<String> participantNames = new ArrayList<>(rooms.get(roomId).keySet());

                // Создаем сообщение с новым статусом
                MessageResponse participantsMessage = new MessageResponse();
                participantsMessage.setMessageStatus(MessageStatus.PARTICIPANTS);
                participantsMessage.setMessage(
                        om.writeValueAsString(new UserMessage("System",roomId, om.writeValueAsString(participantNames))));

                // Отправляем сообщение всем пользователям в комнате
                for (WebSocketSession s : rooms.get(roomId).values()) {
                    if (s.isOpen()) {
                        s.sendMessage(new TextMessage(om.writeValueAsString(participantsMessage)));
                    }
                }
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception)   {
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


//    @PostMapping("/join")
//    public void joinRoom(@RequestBody MessageRequest messageRequest) {
//        System.out.println(messageRequest.getUserName() + " joined the room: " + messageRequest.getRoomId());
//    }
//
//    @PostMapping("/remove")
//    public void leaveRoom(@RequestBody MessageRequest messageRequest) {
//        System.out.println(messageRequest.getUserName() + " left the room: " + messageRequest.getRoomId());
//    }


}
