package org.com.api;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.com.models.messages.MessageStatus;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class MessageRequest {
    private JsonNode message;
    private MessageStatus messageStatus;
    private String userName;

    @Override
    public String toString() {
        return String.format("Message: [%s]", messageStatus);
    }
}

