package org.com.models.messages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.com.api.MessageRequest;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class WebSocketMessage extends MessageRequest {
    private String candidate;
    private String sdpMid;
    private int sdpMLineIndex;
    private String usernameFragment;

}
