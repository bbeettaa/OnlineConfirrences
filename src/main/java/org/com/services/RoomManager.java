package org.com.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.com.models.Room;
import org.com.models.User;
import org.kurento.client.KurentoClient;
import org.kurento.client.WebRtcEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {
    private final Logger log = LoggerFactory.getLogger(RoomManager.class);
    private KurentoClient kurento;
    private final Map<String, Room> rooms;

    public RoomManager(KurentoClient kurento) {
        rooms = new ConcurrentHashMap<>();
        this.kurento = kurento;
    }

    public Room getRoom(String index) {
        return rooms.get(index);
    }



    public User join(String roomId, String userName, WebSocketSession session) throws IOException {
        log.info("ROOM {}: adding participant {}", roomId, userName);
        Room room = getOrCreateRoom(roomId);

        final WebRtcEndpoint webRtcEp = new WebRtcEndpoint.Builder(room.getMediaPipeline()).build();

        User user = new User(userName, room, session, webRtcEp, room.getMediaPipeline());
        user.setMediaPipeline(room.getMediaPipeline());

        final JsonObject newParticipantMsg = new JsonObject();
        newParticipantMsg.addProperty("id", "newParticipantArrived");
        newParticipantMsg.addProperty("name", user.getUserName());
        sendMessageToAllExceptCurrentUser(roomId, session, newParticipantMsg.toString());

        rooms.get(roomId).putUser(user.getUserName(), user);
        sendParticipantNames(user, user.getRoom().getRoomId());
        return user;
    }

    public void leave(User user) throws IOException {
        log.debug("PARTICIPANT {}: Leaving room {}", user.getUserName(), user.getRoom().getRoomId());
        user.getRoom().removeParticipant(user.getUserName());
        user.close();
    }

    public void sendParticipantNames(User user, String roomId) throws IOException {
        final JsonArray participantsArray = new JsonArray();
        for (User p : this.getParticipants(roomId))
            if (!p.equals(user)) {
                JsonElement participantName = new JsonPrimitive(p.getUserName());
                participantsArray.add(participantName);
            }

        JsonObject existingParticipantsMsg = new JsonObject();
        existingParticipantsMsg.addProperty("id", "existingParticipants");
        existingParticipantsMsg.add("data", participantsArray);
        log.debug("PARTICIPANT {}: sending a list of {} participants",
                user.getUserName(), participantsArray.size());
        user.sendMessage(existingParticipantsMsg);
    }


    public void sendMessageToAllExceptCurrentUser(String roomId, WebSocketSession currentSession, String textMessage) {
        sendMessageToAllExceptCurrentUser(roomId, currentSession, new TextMessage(textMessage));
    }

    public void sendMessageToAllExceptCurrentUser(String roomId, WebSocketSession currentSession, TextMessage textMessage) {
        for (User user : rooms.get(roomId).getUsers().values()) {
            WebSocketSession s = user.getSession();
            if (!s.equals(currentSession)) {
                try {
                    s.sendMessage(textMessage);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void broadcastMessageToRoom(String roomId, TextMessage textMessage) {
        for (User user : rooms.get(roomId).getUsers().values()) {
            WebSocketSession s = user.getSession();
            try {
                s.sendMessage(textMessage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void connectUserWebRtcToRoom(User user, String roomId) {
        ConcurrentHashMap<String, User> users = getRoom(roomId).getUsers();
        for (User u : users.values()) {
            if (!u.equals(user)) {
                u.getWebRtcEndpoint().connect(user.getWebRtcEndpoint());
            }
        }
    }

    public Room getOrCreateRoom(String roomId) {
        return rooms.computeIfAbsent(roomId, k ->
                new Room(roomId, kurento.createMediaPipeline())
        );
    }

    public List<String> participantNames(String roomId) {
        return new LinkedList<>(rooms.get(roomId).getUsers().keySet());
    }

//    public MediaPipeline getOrCreate(String roomId) {
//        Room room = rooms.get(roomId);
//        if (room.getMediaPipeline() == null) {
//            room.setMediaPipeline(kurento.createMediaPipeline());
//        }
//        return room.getMediaPipeline();
//    }

    public boolean containsUser(String roomId, String userId) {
        return rooms.get(roomId).getUsers().containsKey(userId);
    }



    public Collection<User> getParticipants(String roomId) {
        return rooms.get(roomId).getUsers().values();
    }
}
