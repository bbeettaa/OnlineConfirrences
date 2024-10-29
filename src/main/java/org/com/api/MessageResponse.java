package org.com.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.com.models.messages.MessageStatus;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class MessageResponse {
    private String userId;
    private String message;
    private MessageStatus messageStatus;

}

