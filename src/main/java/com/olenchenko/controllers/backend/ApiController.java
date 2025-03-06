package com.olenchenko.controllers.backend;

import com.google.gson.Gson;
import com.olenchenko.parser.TouchParser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiController {
    TouchParser touchParser;

    public ApiController() {
        this.touchParser = new TouchParser();
    }

    @GetMapping("/api/test")
    public String getData() {
        return "Ok";
    }
    @GetMapping(value = "/api/newproducts", produces = "application/json")
    public String getMainPage() {
        Gson gson = new Gson();
        String res = gson.toJson(touchParser.getNewProducts());
        return res;
    }
}
