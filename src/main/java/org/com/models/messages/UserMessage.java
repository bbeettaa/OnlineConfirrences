package org.com.models.messages;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class UserMessage {
    private String userName;
    private String roomId;
    private String message;

    @Override
    public String toString() {
        return "MessageRequest{" +
                "userName='" + userName + '\'' +
                ", roomId='" + roomId + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
