package org.com.controllers;

import org.com.models.Room;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    private Map<String, Room> rooms = new HashMap<>();

    @PostMapping
    public Room createRoom(@RequestBody Room room) {
        rooms.put(room.getRoomId(), room);
        return room;
    }

    @GetMapping("/{roomId}")
    public Room getRoom(@PathVariable(name = "roomId") String roomId)  {
        return rooms.get(roomId);
    }
}
