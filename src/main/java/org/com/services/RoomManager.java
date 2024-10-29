package org.com.services;

import org.com.models.Room;
import org.com.models.User;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {
    private KurentoClient kurento;
    private final Map<String, Room> rooms;

    public RoomManager(KurentoClient kurento) {
        rooms = new ConcurrentHashMap<>();
        this.kurento = kurento;
    }

    public Room getRoom(String index) {
        return rooms.get(index);
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
        return rooms.computeIfAbsent(roomId, k -> new Room(roomId));
    }

    public List<String> participantNames(String roomId) {
        return new LinkedList<>(rooms.get(roomId).getUsers().keySet());
    }

    public MediaPipeline getOrCreate(String roomId) {
        Room room = rooms.get(roomId);
        if (room.getMediaPipeline() == null) {
            room.setMediaPipeline(kurento.createMediaPipeline());
        }
        return room.getMediaPipeline();
    }

    public boolean containsUser(String roomId, String userId) {
        return rooms.get(roomId).getUsers().containsKey(userId);
    }

    public User removeUser(String roomId, String userId) {
        User u = rooms.get(roomId).getUser(userId);
        if (u != null)
            rooms.get(roomId).remove(userId);
        return u;
    }

}
