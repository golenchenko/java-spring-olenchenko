package com.olenchenko;

import com.google.gson.Gson;
import com.olenchenko.parser.TouchParser;

public class Main {
    public static void main(String[] args) {
        TouchParser touchParser = new TouchParser();
        Gson gson = new Gson();
        System.out.println(gson.toJson(touchParser.getNewProducts()));
    }
}