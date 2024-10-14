package org.com.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Room {
    private String roomId;

    public Room(String roomId) {
        this.roomId = roomId;
    }

}
