package org.com.models;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.kurento.client.Continuation;
import org.kurento.client.MediaPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RequiredArgsConstructor()
@Getter
public class Room implements Closeable {
    private final Logger log = LoggerFactory.getLogger(Room.class);
    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();

    private final String roomId;
    private final MediaPipeline mediaPipeline;

    public User getUser(String userId){
        return users.get(userId);
    }

    public User putUser(String userId, User user){
        return users.put(userId, user);
    }



    public Collection<String> getUserNameList(){
        return getUsers().values().stream().map(User::getUserName).collect(Collectors.toList());
    }

    public Collection<User> getUserList(){
        return getUsers().values();
    }




    public void removeParticipant(String name) throws IOException {
        users.remove(name);

        log.debug("ROOM {}: notifying all users that {} is leaving the room", this.roomId, name);

        final List<String> unnotifiedParticipants = new ArrayList<>();
        final JsonObject participantLeftJson = new JsonObject();
        participantLeftJson.addProperty("id", "participantLeft");
        participantLeftJson.addProperty("name", name);
        for (User participant : users.values()) {
            try {
                participant.cancelVideoFrom(name);
                participant.sendMessage(participantLeftJson);
            } catch (final IOException e) {
                unnotifiedParticipants.add(participant.getUserName());
            }
        }

        if (!unnotifiedParticipants.isEmpty()) {
            log.debug("ROOM {}: The users {} could not be notified that {} left the room", this.roomId,
                    unnotifiedParticipants, name);
        }

    }



    @Override
    public void close() {
        for (User user : getUserList()) {
            try {
                user.close();
            } catch (IOException e) {
                log.debug("ROOM {}: Could not invoke close on participant {}", this.roomId, user.getUserName(), e);
            }
        }

        users.clear();
        mediaPipeline.release(new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) throws Exception {
                log.trace("ROOM {}: Released Pipeline", Room.this.roomId);
            }
            @Override
            public void onError(Throwable cause) throws Exception {
                log.warn("PARTICIPANT {}: Could not release Pipeline", Room.this.roomId);
            }
        });

        log.debug("Room {} closed", this.roomId);
    }

}
