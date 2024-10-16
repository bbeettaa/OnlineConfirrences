package org.com.models.api;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.com.models.MessageStatus;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class MessageRequest {
    private JsonNode message;
    private MessageStatus messageStatus;

}

