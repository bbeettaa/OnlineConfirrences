package org.com.models;

import com.google.gson.JsonObject;
import lombok.*;
import org.com.models.messages.IceCandidateMessage;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

//@AllArgsConstructor
//@RequiredArgsConstructor
//@NoArgsConstructor
@Getter
@Setter
public class User implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(User.class);
    @NonNull
    private String userName;
    @NonNull
    private WebSocketSession session;
    private WebRtcEndpoint webRtcEndpoint;
    private MediaPipeline mediaPipeline;
    private Room room;

    private final ConcurrentMap<String, WebRtcEndpoint> incomingMedia = new ConcurrentHashMap<>();


    public User(String userName, Room room, WebSocketSession session, WebRtcEndpoint webRtcEndpoint, MediaPipeline mediaPipeline) {
        this.userName = userName;
        this.room = room;
        this.session = session;
        this.webRtcEndpoint = webRtcEndpoint;
        this.mediaPipeline = mediaPipeline;

        webRtcEndpoint.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

            @Override
            public void onEvent(IceCandidateFoundEvent event) {
                JsonObject response = new JsonObject();
                response.addProperty("id", "iceCandidate");
                response.addProperty("name", userName);
                response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
                try {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(response.toString()));
                    }
                } catch (IOException e) {
                    log.debug(e.getMessage());
                }
            }
        });
    }





    public void receiveVideoFrom(User sender, String sdpOffer) throws IOException {
        log.info("USER {}: connecting with {} in room {}", this.userName, sender.getUserName(), room.getRoomId());
//        log.trace("USER {}: SdpOffer for {} is {}", this.userName, sender.getUserName(), sdpOffer);

        final String ipSdpAnswer = this.getEndpointForUser(sender).processOffer(sdpOffer);
        final JsonObject scParams = new JsonObject();
        scParams.addProperty("id", "receiveVideoAnswer");
        scParams.addProperty("name", sender.getUserName());
        scParams.addProperty("sdpAnswer", ipSdpAnswer);

        log.trace("USER {}: SdpAnswer for {} is {}", this.userName, sender.userName, ipSdpAnswer);
        this.sendMessage(scParams);
        log.debug("gather candidates");
        this.getEndpointForUser(sender).gatherCandidates();
    }

    private WebRtcEndpoint getEndpointForUser(final User sender) {
        if (sender.getUserName().equals(userName)) {
            log.debug("PARTICIPANT {}: configuring loopback", this.userName);
            return webRtcEndpoint;
        }

        log.debug("PARTICIPANT {}: receiving video from {}", this.userName, sender.getUserName());

        WebRtcEndpoint incoming = incomingMedia.get(sender.getUserName());
        if (incoming == null) {
            log.debug("PARTICIPANT {}: creating new endpoint for {}", this.userName, sender.getUserName());
            incoming = new WebRtcEndpoint.Builder(mediaPipeline).build();

            incoming.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

                @Override
                public void onEvent(IceCandidateFoundEvent event) {
                    JsonObject response = new JsonObject();
                    response.addProperty("id", "iceCandidate");
                    response.addProperty("name", sender.getUserName());
                    response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
                    try {
                        synchronized (session) {
                            session.sendMessage(new TextMessage(response.toString()));
                        }
                    } catch (IOException e) {
                        log.debug(e.getMessage());
                    }
                }
            });

            incomingMedia.put(sender.getUserName(), incoming);
        }

        log.debug("PARTICIPANT {}: obtained endpoint for {}", this.userName, sender.getUserName());
        sender.getWebRtcEndpoint().connect(incoming);

        return incoming;
    }

    public void cancelVideoFrom(final User sender) {
        this.cancelVideoFrom(sender.getUserName());
    }

    public void cancelVideoFrom(final String senderName) {
        log.debug("PARTICIPANT {}: canceling video reception from {}", this.getUserName(), senderName);
        final WebRtcEndpoint incoming = incomingMedia.remove(senderName);

        log.debug("PARTICIPANT {}: removing endpoint for {}", this.getUserName(), senderName);
        incoming.release(new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) throws Exception {
                log.trace("PARTICIPANT {}: Released successfully incoming EP for {}",
                        userName, senderName);
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.warn("PARTICIPANT {}: Could not release incoming EP for {}",
                        userName, senderName);
            }
        });
    }


    public void sendMessage(JsonObject message) throws IOException {
        log.debug("USER {}: Sending message {}", userName, message);
        synchronized (session) {
            session.sendMessage(new TextMessage(message.toString()));
        }
    }

    public void addCandidate(IceCandidate candidate, String userName) {
        if (this.userName.compareTo(userName) == 0) {
            webRtcEndpoint.addIceCandidate(candidate);
        } else {
            WebRtcEndpoint webRtc = incomingMedia.get(userName);
            if (webRtc != null) {
                webRtc.addIceCandidate(candidate);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof User)) {
            return false;
        }
        User other = (User) obj;
        boolean eq = userName.equals(other.userName);
        eq &= room.getRoomId().equals(other.room.getRoomId());
        return eq;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + userName.hashCode();
        result = 31 * result + room.getRoomId().hashCode();
        return result;
    }


    @Override
    public void close() throws IOException {
        log.debug("PARTICIPANT {}: Releasing resources", this.userName);
        for (final String remoteParticipantName : incomingMedia.keySet()) {

            log.trace("PARTICIPANT {}: Released incoming EP for {}", this.userName, remoteParticipantName);

            final WebRtcEndpoint ep = this.incomingMedia.get(remoteParticipantName);

            ep.release(new Continuation<Void>() {
                @Override
                public void onSuccess(Void result) throws Exception {
                    log.trace("PARTICIPANT {}: Released successfully incoming EP for {}",
                            User.this.userName, remoteParticipantName);
                }

                @Override
                public void onError(Throwable cause) throws Exception {
                    log.warn("PARTICIPANT {}: Could not release incoming EP for {}", User.this.userName,
                            remoteParticipantName);
                }
            });
        }

        webRtcEndpoint.release(new Continuation<Void>() {

            @Override
            public void onSuccess(Void result) throws Exception {
                log.trace("PARTICIPANT {}: Released outgoing EP", User.this.userName);
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.warn("USER {}: Could not release outgoing EP", User.this.userName);
            }
        });
    }
}
