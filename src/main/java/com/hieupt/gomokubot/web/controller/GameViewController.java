package com.hieupt.gomokubot.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GameViewController {

    @GetMapping("/")
    public String index() {
        return "index";
    }
}

