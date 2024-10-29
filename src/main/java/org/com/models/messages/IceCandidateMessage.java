package org.com.models.messages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter

public class IceCandidateMessage {
    private String candidate;
    private String sdpMid;
    private int sdpMLineIndex;
    private String usernameFragment;
}