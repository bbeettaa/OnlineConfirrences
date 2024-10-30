package org.com.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.com.api.MessageRequest;
import org.com.api.MessageResponse;
import org.com.models.Room;
import org.com.models.User;
import org.com.models.messages.*;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.*;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;

public class OnlineRoomWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(OnlineRoomWebSocketHandler.class);
    private static final Gson gson = new GsonBuilder().create();
    private final ObjectMapper om = new ObjectMapper();

//    private final ConcurrentHashMap<String, UserSession> users = new ConcurrentHashMap<>();

    @Autowired
    private RoomManager roomManager;

    @Autowired
    private KurentoClient kurento;

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        try {
            MessageRequest payload = om.readValue(message.getPayload().toString(), MessageRequest.class);
            System.out.println("Получено сообщение: " + payload);
            User user = getUserFromSession(session);

            switch (payload.getMessageStatus()) {
                //joinRoom
                case ESTABLISHING:
                    handleEstablishing(session, payload);
                    break;

                case RECEIVE_VIDEO_FROM:
                    ReceiveVideoMessage sdpMsg = om.readValue(payload.getMessage().toString(), ReceiveVideoMessage.class);

                    String senderName = sdpMsg.getSender();
                    User sender = roomManager.getRoom(getRoomIdFromSession(session)).getUser(senderName);
                    String sdpOffer = sdpMsg.getSdpOffer();
                    user.receiveVideoFrom(sender, sdpOffer);
                    break;

//                case SDP_OFFER:
//                    handleSdpOffer(session, payload);
//                    break;

                case ADD_ICE_CANDIDATE:
                    IceCandidateMessage iceMsg = om.readValue(payload.getMessage().toString(), IceCandidateMessage.class);
                    if (user != null) {
                    IceCandidate candidate = new IceCandidate(iceMsg.getCandidate(),
                            iceMsg.getSdpMid(), iceMsg.getSdpMLineIndex());
                    user.addCandidate(candidate, payload.getUserName() );
                }
                    break;
                case MESSAGE:
                    handleMessage(session, payload);
                    break;

                case LEAVE_ROOM:
                    roomManager.leave(user);
                    break;

                case ERROR:
                    log.error(payload.getMessage().toString());
                    break;

            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }



    private void handleMessage(WebSocketSession session, MessageRequest payload) throws IOException {
        log.error("MESSAGE REQUESTED: " + payload);
//        UserMessage messageRequest = om.readValue(payload.getMessage().toString(), UserMessage.class);
//        String roomId = messageRequest.getRoomId();

//        TextMessage textMessage = new TextMessage(om.writeValueAsString(new MessageResponse((String) session.getAttributes().get("userName"), om.writeValueAsString(new UserMessage(messageRequest.getUserName(), messageRequest.getRoomId(), messageRequest.getUserName() + ": " + messageRequest.getMessage())), MessageStatus.MESSAGE)));
//        roomManager.broadcastMessageToRoom(roomId, textMessage);
    }

    private void handleEstablishing(WebSocketSession session, MessageRequest payload) throws Exception {
        UserMessage messageRequest = om.readValue(payload.getMessage().toString(), UserMessage.class);

        session.getAttributes().put("userName", messageRequest.getUserName());
        session.getAttributes().put("roomId", messageRequest.getRoomId());
        log.info("PARTICIPANT {}: trying to join room {}", getUserIdFromSession(session), getRoomIdFromSession(session));

        User user = roomManager.join(getRoomIdFromSession(session), getUserIdFromSession(session), session);
        session.getAttributes().put("user", user);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("[HelloWorldHandler::handleTransportError] Exception: {}, userId: {}", exception, getUserIdFromSession(session));
        session.close(CloseStatus.SERVER_ERROR);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        User user = getUserFromSession(session);
        roomManager.leave(user);
    }

    private synchronized void sendMessage(final WebSocketSession session, String message) {
        log.debug("[HelloWorldHandler::sendMessage] {}", message);

        if (!session.isOpen()) {
            log.warn("[HelloWorldHandler::sendMessage] Skip, WebSocket session isn't open");
            return;
        }

        final String userId = getUserIdFromSession(session);
        if (!roomManager.containsUser(getRoomIdFromSession(session), userId)) {
            log.warn("[HelloWorldHandler::sendMessage] Skip, unknown user, id: {}", userId);
            return;
        }

        try {
            session.sendMessage(new TextMessage(message));
        } catch (IOException ex) {
            log.error("[HelloWorldHandler::sendMessage] Exception: {}", ex.getMessage());
        }
    }




    public String getUserIdFromSession(WebSocketSession session) {
        return String.valueOf(session.getAttributes().get("userName"));
    }

    public String getRoomIdFromSession(WebSocketSession session) {
        return String.valueOf(session.getAttributes().get("roomId"));
    }

    public User getUserFromSession(WebSocketSession session) {
        return (User) session.getAttributes().get("user");
    }


}


