package org.com.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class MessageRequest {
    private String userName;
    private String roomId;
    private String message;
    private MessageStatus messageStatus;

    @Override
    public String toString() {
        return "MessageRequest{" +
                "userName='" + userName + '\'' +
                ", roomId='" + roomId + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}

