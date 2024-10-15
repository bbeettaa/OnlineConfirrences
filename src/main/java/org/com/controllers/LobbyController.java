package org.com.controllers;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LobbyController {

    @GetMapping("/")
    public String showLobby() {
        return "room"; // Возвращает страницу lobby
    }


}
