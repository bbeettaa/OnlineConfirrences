package org.com.models;

import lombok.*;
import org.com.models.messages.IceCandidateMessage;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@RequiredArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class User {
    @NonNull
    private String userName;
    @NonNull
    private WebSocketSession session;
    private WebRtcEndpoint webRtcEndpoint;
    private MediaPipeline mediaPipeline ;
}
