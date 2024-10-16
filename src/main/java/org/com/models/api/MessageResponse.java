package org.com.models.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.com.models.MessageStatus;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class MessageResponse {
    private String message;
    private MessageStatus messageStatus;

}

