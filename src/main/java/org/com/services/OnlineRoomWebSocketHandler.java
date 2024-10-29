package org.com.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.com.api.MessageRequest;
import org.com.api.MessageResponse;
import org.com.models.Room;
import org.com.models.User;
import org.com.models.messages.SdpMessage;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.*;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.com.models.messages.UserMessage;
import org.com.models.messages.IceCandidateMessage;
import org.com.models.messages.MessageStatus;

import java.util.List;

public class OnlineRoomWebSocketHandler implements WebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(OnlineRoomWebSocketHandler.class);
    private static final Gson gson = new GsonBuilder().create();
    private final ObjectMapper om = new ObjectMapper();

//    private final ConcurrentHashMap<String, UserSession> users = new ConcurrentHashMap<>();

    @Autowired
    private RoomManager roomManager;

    @Autowired
    private KurentoClient kurento;


    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String roomId = getRoomIdFromSession(session);
        System.out.println("Session established for room: " + roomId);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        try {
            MessageRequest payload = om.readValue(message.getPayload().toString(), MessageRequest.class);
            System.out.println("Получено сообщение: " + payload);

            switch (payload.getMessageStatus()) {
                case ESTABLISHING:
                    handleEstablishing(session, payload);
                    break;
                case SDP_OFFER:
                    handleSdpOffer(session, payload);
                    break;
                case ADD_ICE_CANDIDATE:
                    handleIceCandidate(session, payload);
                    break;
                case MESSAGE:
                    handleMessage(session, payload);
                    break;
                case ERROR:
                    log.error(payload.getMessage().toString());
                    break;

            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    private void handleSdpOffer(WebSocketSession session, MessageRequest payload) throws IOException {
        SdpMessage message = om.readValue(payload.getMessage().toString(), SdpMessage.class);

        final String userId = getUserIdFromSession(session);
        final String roomId = getRoomIdFromSession(session);
        log.info("[HelloWorldHandler::handleStart] User count: {}", roomManager.getRoom(getRoomIdFromSession(session)).getUsers().size());
        log.info("[HelloWorldHandler::handleStart] New user, id: {}", userId);


        WebRtcEndpoint webRtcEp = roomManager.getRoom(roomId).getUser(getUserIdFromSession(session)).getWebRtcEndpoint();
        // ---- connect webRtc
//        roomManager.connectUserWebRtcToRoom(roomManager.getRoom(roomId).getUser(userId), roomId);
        for (User u : roomManager.getRoom(roomId).getUsers().values()) {
            if (!u.getUserName().equals(userId)) {
                u.getWebRtcEndpoint().connect(webRtcEp);
                webRtcEp.setName(getUserIdFromSession(u.getSession()));

            }
        }
        // ---- Endpoint configuration
        initWebRtcEndpoint(session, webRtcEp);
        // Continue the SDP Negotiation: Generate an SDP Answer
        String sdpAnswer =  webRtcEp.processOffer(message.getSdp());
        log.info("[HelloWorldHandler::handleStart] New WebRtcEndpoint: {}", webRtcEp.getName());

        // ---- Endpoint startup
        webRtcEp.gatherCandidates();
        // ---- Send webrtc
        String answer = gson.toJson(new MessageResponse(getUserIdFromSession(session), sdpAnswer, MessageStatus.PROCESS_SDP_ANSWER));
        sendMessage(session, answer);
//        roomManager.sendMessageToAllExceptCurrentUser(getRoomIdFromSession(session), session, answer);
    }

    private void initWebRtcEndpoint(final WebSocketSession session, final WebRtcEndpoint webRtcEp) {
        initBaseEventListeners(session, webRtcEp, "WebRtcEndpoint");
        initWebRtcEventListeners(session, webRtcEp);


//        log.info("[HelloWorldHandler::initWebRtcEndpoint] name: {}, SDP Offer from browser to KMS:\n{}", name, sdpOffer);
//        log.info("[HelloWorldHandler::initWebRtcEndpoint] name: {}, SDP Answer from KMS to browser:\n{}", name, sdpAnswer);

//        message.addProperty("messageStatus", "PROCESS_SDP_ANSWER");
//        message.addProperty("sdpAnswer", sdpAnswer);
    }


    private void handleIceCandidate(WebSocketSession session, MessageRequest payload) throws IOException {
        IceCandidateMessage message = om.readValue(payload.getMessage().toString(), IceCandidateMessage.class);

        final String userId = getUserIdFromSession(session);
        final String roomId = getRoomIdFromSession(session);

        User user = roomManager.getRoom(roomId).getUser(userId);
        IceCandidate candidate = new IceCandidate(message.getCandidate(), message.getSdpMid(), message.getSdpMLineIndex());

        WebRtcEndpoint webRtcEp = user.getWebRtcEndpoint();
        webRtcEp.addIceCandidate(candidate);
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
        Room room = roomManager.getOrCreateRoom(messageRequest.getRoomId());
        room.putUser(messageRequest.getUserName(), new User(messageRequest.getUserName(), session));


        final User user = new User(messageRequest.getUserName(), session);
        roomManager.getRoom(messageRequest.getRoomId()).putUser(user.getUserName(),user);
        // ---- Media pipeline
        log.info("[HelloWorldHandler::handleStart] Create Media Pipeline");
        final MediaPipeline pipeline = roomManager.getOrCreate(getRoomIdFromSession(session));
        user.setMediaPipeline(pipeline);
        final WebRtcEndpoint webRtcEp = new WebRtcEndpoint.Builder(pipeline).build();
        user.setWebRtcEndpoint(webRtcEp);

        // Ответить клиенту
        session.sendMessage(
                new TextMessage(
                        om.writeValueAsString(
                                new MessageResponse(
                                        getUserIdFromSession(session),
                                        om.writeValueAsString(
                                                new UserMessage(
                                                        messageRequest.getUserName(),
                                                        messageRequest.getRoomId(),
                                                        "Привет " + messageRequest.getUserName() + " от сервера! Ты в комнате. " + messageRequest.getRoomId())), MessageStatus.MESSAGE))));

        // Отправка ICE-кандидатов новому участнику от всех остальных
//        for (User user : roomManager.getRoom(messageRequest.getRoomId()).getUsers().values())
//            session.sendMessage(new TextMessage(om.writeValueAsString(new MessageResponse((String) session.getAttributes().get("userName"), om.writeValueAsString(user.getIceCandidate()), MessageStatus.ICE_CANDIDATE))));


//        room.putUser(messageRequest.getUserName(), new User(messageRequest.getUserName(), session, null));

        notifyParticipants(session, messageRequest.getRoomId());
        System.out.println("Пользователь: " + messageRequest.getUserName() + " присоединился к комнате: " + messageRequest.getRoomId());
    }

    private void notifyParticipants(WebSocketSession session, String roomId) throws Exception {
        List<String> participantNames = roomManager.participantNames(roomId);

        // Создаем сообщение с новым статусом
        MessageResponse participantsMessage = new MessageResponse();
        participantsMessage.setMessageStatus(MessageStatus.PARTICIPANTS);
        participantsMessage.setMessage(om.writeValueAsString(
                new UserMessage(getUserIdFromSession(session), roomId, om.writeValueAsString(participantNames))));
        participantsMessage.setUserId((String) session.getAttributes().get("userName"));
        // Отправляем сообщение всем пользователям в комнате
//        for (WebSocketSession s : rooms.get(roomId).values()) {
//            if (s.isOpen()) {
//                s.sendMessage(new TextMessage(om.writeValueAsString(participantsMessage)));
//            }
//        }

        TextMessage textMessage = new TextMessage(om.writeValueAsString(participantsMessage));
        roomManager.broadcastMessageToRoom(roomId, textMessage);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("[HelloWorldHandler::handleTransportError] Exception: {}, userId: {}", exception, getUserIdFromSession(session));
        session.close(CloseStatus.SERVER_ERROR);
    }

    @Override
    public void afterConnectionClosed(final WebSocketSession session, CloseStatus status) throws Exception {
        if (!status.equalsCode(CloseStatus.NORMAL)) {
            log.warn("[HelloWorldHandler::afterConnectionClosed] status: {}, userId: {}", status, getUserIdFromSession(session));
        }
        roomManager.getRoom(getRoomIdFromSession(session)).remove(getUserIdFromSession(session));

        stop(session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    private synchronized void sendMessage(final WebSocketSession session, String message) {
        log.debug("[HelloWorldHandler::sendMessage] {}", message);

        if (!session.isOpen()) {
            log.warn("[HelloWorldHandler::sendMessage] Skip, WebSocket session isn't open");
            return;
        }

        final String userId = getUserIdFromSession(session);
        if (!roomManager.containsUser(getRoomIdFromSession(session),userId)) {
            log.warn("[HelloWorldHandler::sendMessage] Skip, unknown user, id: {}", userId);
            return;
        }

        try {
            session.sendMessage(new TextMessage(message));
        } catch (IOException ex) {
            log.error("[HelloWorldHandler::sendMessage] Exception: {}", ex.getMessage());
        }
    }

    private void sendError(final WebSocketSession session, String errMsg) {
        log.error(errMsg);

        if (roomManager.containsUser(getRoomIdFromSession(session),getUserIdFromSession(session))) {
            JsonObject message = new JsonObject();
            message.addProperty("messageStatus", "ERROR");
            message.addProperty("message", errMsg);
            sendMessage(session, message.toString());
        }
    }

    private void stop(final WebSocketSession session) {
        // Remove the user session and release all resources
        final User user = roomManager.removeUser(getRoomIdFromSession(session), getUserIdFromSession(session));
        if (user != null) {
            MediaPipeline mediaPipeline = user.getMediaPipeline();
            if (mediaPipeline != null) {
                log.info("[HelloWorldHandler::stop] Release the Media Pipeline");
                mediaPipeline.release();
            }
        }
    }

    private void handleStop(final WebSocketSession session, JsonObject jsonMessage) {
        stop(session);
    }

    private void initBaseEventListeners(final WebSocketSession session, BaseRtpEndpoint baseRtpEp, final String className) {
        log.info("[HelloWorldHandler::initBaseEventListeners] name: {}, class: {}, userId: {}",
                baseRtpEp.getName(), className, getUserIdFromSession(session));

        // Event: Some error happened
        baseRtpEp.addErrorListener(new EventListener<ErrorEvent>() {
            @Override
            public void onEvent(ErrorEvent ev) {
                log.error("[{}::ErrorEvent] Error code {}: '{}', source: {}, timestamp: {}, tags: {}, description: {}",
                        className, ev.getErrorCode(), ev.getType(), ev.getSource().getName(), ev.getTimestampMillis(),
                        ev.getTags(), ev.getDescription());

                sendError(session, "[Kurento] " + ev.getDescription());
                stop(session);
            }
        });

        // Event: Media is flowing into this sink
        baseRtpEp.addMediaFlowInStateChangedListener(new EventListener<MediaFlowInStateChangedEvent>() {
            @Override
            public void onEvent(MediaFlowInStateChangedEvent ev) {
                log.info("[{}::{}] source: {}, timestamp: {}, tags: {}, state: {}, padName: {}, mediaType: {}", className, ev.getType(), ev.getSource().getName(), ev.getTimestampMillis(), ev.getTags(), ev.getState(), ev.getPadName(), ev.getMediaType());
            }
        });

        // Event: Media is flowing out of this source
        baseRtpEp.addMediaFlowOutStateChangedListener(new EventListener<MediaFlowOutStateChangedEvent>() {
            @Override
            public void onEvent(MediaFlowOutStateChangedEvent ev) {
                log.info("[{}::{}] source: {}, timestamp: {}, tags: {}, state: {}, padName: {}, mediaType: {}", className, ev.getType(), ev.getSource().getName(), ev.getTimestampMillis(), ev.getTags(), ev.getState(), ev.getPadName(), ev.getMediaType());
            }
        });

        // Event: [TODO write meaning of this event]
        baseRtpEp.addConnectionStateChangedListener(new EventListener<ConnectionStateChangedEvent>() {
            @Override
            public void onEvent(ConnectionStateChangedEvent ev) {
                log.info("[{}::{}] source: {}, timestamp: {}, tags: {}, oldState: {}, newState: {}", className, ev.getType(), ev.getSource().getName(), ev.getTimestampMillis(), ev.getTags(), ev.getOldState(), ev.getNewState());
            }
        });

        // Event: [TODO write meaning of this event]
        baseRtpEp.addMediaStateChangedListener(new EventListener<MediaStateChangedEvent>() {
            @Override
            public void onEvent(MediaStateChangedEvent ev) {
                log.info("[{}::{}] source: {}, timestamp: {}, tags: {}, oldState: {}, newState: {}", className, ev.getType(), ev.getSource().getName(), ev.getTimestampMillis(), ev.getTags(), ev.getOldState(), ev.getNewState());
            }
        });

        // Event: This element will (or will not) perform media transcoding
        baseRtpEp.addMediaTranscodingStateChangedListener(new EventListener<MediaTranscodingStateChangedEvent>() {
            @Override
            public void onEvent(MediaTranscodingStateChangedEvent ev) {
                log.info("[{}::{}] source: {}, timestamp: {}, tags: {}, state: {}, binName: {}, mediaType: {}", className, ev.getType(), ev.getSource().getName(), ev.getTimestampMillis(), ev.getTags(), ev.getState(), ev.getBinName(), ev.getMediaType());
            }
        });
    }

    private void initWebRtcEventListeners(final WebSocketSession session, final WebRtcEndpoint webRtcEp) {
        log.info("[HelloWorldHandler::initWebRtcEventListeners] name: {}, userId: {}", webRtcEp.getName(), getUserIdFromSession(session));

        // Event: The ICE backend found a local candidate during Trickle ICE
        webRtcEp.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {
            @Override
            public void onEvent(IceCandidateFoundEvent ev) {
                log.debug("[WebRtcEndpoint::{}] source: {}, timestamp: {}, tags: {}, candidate: {}", ev.getType(), ev.getSource().getName(), ev.getTimestampMillis(), ev.getTags(), JsonUtils.toJson(ev.getCandidate()));

//                JsonObject message = new JsonObject();
//                message.addProperty("messageStatus", "ADD_ICE_CANDIDATE");
//                message.add("candidate", JsonUtils.toJsonObject(ev.getCandidate()));
//                sendMessage(session, message.toString());

                try{
                    String answer = gson.toJson(new MessageResponse(
                            ev.getSource().getName(),
                            om.writeValueAsString(ev.getCandidate()),
                            MessageStatus.ADD_ICE_CANDIDATE));
                    sendMessage(session, answer);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        // Event: The ICE backend changed state
        webRtcEp.addIceComponentStateChangedListener(new EventListener<IceComponentStateChangedEvent>() {
            @Override
            public void onEvent(IceComponentStateChangedEvent ev) {
                log.debug("[WebRtcEndpoint::{}] source: {}, timestamp: {}, tags: {}, streamId: {}, componentId: {}, state: {}", ev.getType(), ev.getSource().getName(), ev.getTimestampMillis(), ev.getTags(), ev.getStreamId(), ev.getComponentId(), ev.getState());
            }
        });

        // Event: The ICE backend finished gathering ICE candidates
        webRtcEp.addIceGatheringDoneListener(new EventListener<IceGatheringDoneEvent>() {
            @Override
            public void onEvent(IceGatheringDoneEvent ev) {
                log.info("[WebRtcEndpoint::{}] source: {}, timestamp: {}, tags: {}", ev.getType(), ev.getSource().getName(), ev.getTimestampMillis(), ev.getTags());
            }
        });

        // Event: The ICE backend selected a new pair of ICE candidates for use
        webRtcEp.addNewCandidatePairSelectedListener(new EventListener<NewCandidatePairSelectedEvent>() {
            @Override
            public void onEvent(NewCandidatePairSelectedEvent ev) {
                log.info("[WebRtcEndpoint::{}] name: {}, timestamp: {}, tags: {}, streamId: {}, local: {}, remote: {}", ev.getType(), ev.getSource().getName(), ev.getTimestampMillis(), ev.getTags(), ev.getCandidatePair().getStreamId(), ev.getCandidatePair().getLocalCandidate(), ev.getCandidatePair().getRemoteCandidate());
            }
        });
    }


    public String getUserIdFromSession(WebSocketSession session) {
        return String.valueOf(session.getAttributes().get("userName"));
    }

    public String getRoomIdFromSession(WebSocketSession session) {
        return String.valueOf(session.getAttributes().get("roomId"));
    }


}


