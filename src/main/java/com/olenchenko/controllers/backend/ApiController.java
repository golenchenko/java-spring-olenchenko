package com.olenchenko.controllers.backend;

import com.google.gson.Gson;
import com.olenchenko.parser.TouchParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;

@RestController
public class ApiController {


    TouchParser touchParser;
    private List<String> sortFields = List.of("SHOWS", "PRICE_ASC", "PRICE_DESC", "DATE");

//    @Autowired - Field injection is not recommended.
//    Constructor injection is preferred as it allows for better testability and immutability.
//    Field injection hides the dependencies of the class, making it unclear what dependencies
//    the class relies on. This can make the class harder to understand and maintain.
//    Constructor injection, on the other hand, makes dependencies explicit, as they are
//    declared in the constructor.
//    https://www.geeksforgeeks.org/why-is-field-injection-not-recommended-in-spring/
    @Autowired
    public ApiController(TouchParser touchParser) {
        this.touchParser = touchParser;
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

    @GetMapping(value="/api/search", produces = "application/json")
    public String search(@RequestParam String q,
                         @RequestParam(required = false, defaultValue = "-1") int page_number,
                         @RequestParam(required = false, defaultValue = "SHOWS") String sort_field,
                         @RequestParam(required = false, defaultValue = "-1") int min_price,
                         @RequestParam(required = false, defaultValue = "-1") int max_price
    ) {
        Gson gson = new Gson();
        boolean isFilterEnabled = false;
        HashMap<String, String> filters = new HashMap<>();
        String sortFieldInUpperCase = sort_field.toUpperCase();
        if (page_number > 1) {
            filters.put("PAGEN_1", String.valueOf(page_number));
        }
        if (sortFields.contains(sortFieldInUpperCase)) {
            filters.put("SORT_FIELD", sort_field);
        } else {
            filters.put("SORT_FIELD", "SHOWS");
        }

        if (max_price < min_price) {
            int temp = min_price;
            min_price = max_price;
            max_price = temp;
        }

        if (min_price >= 0) {
            filters.put("arrFilterFilter_P1_MIN", String.valueOf(min_price));
            isFilterEnabled = true;
        }
        if (max_price >= 0) {
            filters.put("arrFilterFilter_P1_MAX", String.valueOf(max_price));
            isFilterEnabled = true;
        }

        if (isFilterEnabled) {
            filters.put("set_filter", "y");
        }
        return gson.toJson(touchParser.getProductsFromQuery(q, filters));
    }

}
