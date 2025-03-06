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
        return gson.toJson(touchParser.getNewProducts());
    }
    @GetMapping(value = "/api/mergedcategories", produces = "application/json")
    public String getMergedCategoriesFromMainPage() {
        Gson gson = new Gson();
        return gson.toJson(touchParser.getMergedCategories());
    }
    @GetMapping(value = "/api/sales", produces = "application/json")
    public String getSales() {
        Gson gson = new Gson();
        return gson.toJson(touchParser.getSales());
    }
    @GetMapping(value = "/api/markdown", produces = "application/json")
    public String getMarkdown() {
        Gson gson = new Gson();
        return gson.toJson(touchParser.getMarkdown());
    }
    @GetMapping(value = "/api/bestsellers", produces = "application/json")
    public String getBestSellers() {
        Gson gson = new Gson();
        return gson.toJson(touchParser.getBestSellers());
    }
    @GetMapping(value = "/api/refreshdata", produces = "application/json")
    public String refreshData() {
        touchParser.refreshMainPage();
        Gson gson = new Gson();
        return gson.toJson("Data refreshed");
    }

}
