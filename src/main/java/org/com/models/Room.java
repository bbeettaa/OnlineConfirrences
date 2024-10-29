package org.com.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.kurento.client.MediaPipeline;

import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor()
@Getter
public class Room {
    private final String roomId;
    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    @Setter
    private MediaPipeline mediaPipeline;

    public User getUser(String userId){
        return users.get(userId);
    }

    public User putUser(String userId, User user){
        return users.put(userId, user);
    }

    public void remove(String userName){
        users.remove(userName);
    }

}
