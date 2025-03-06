package com.olenchenko.controllers.frontend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PagesController {
    private final static String newProductsText = "Новинка";
    private final static String bestSellersText = "Хіт продажів";
    private final static String markdownText = "Уцінка";
    private final static String salesText = "Розпродаж";
    private final String apiUrl;
    public PagesController(@Value("${api}") String apiUrl) {
        this.apiUrl = apiUrl;
    }

    @GetMapping("/")
    public String index(Model model) {
        // Get data from ApiController about new products
        String url = apiUrl + "mergedcategories";
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<HashMap<String, List<HashMap<String, Object>>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );
        HashMap<String, List<HashMap<String, Object>>> products = response.getBody();
        products.forEach((key, value) -> {
            switch (key) {
                case newProductsText -> model.addAttribute("newproducts", value);
                case bestSellersText -> model.addAttribute("bestsellers", value);
                case markdownText -> model.addAttribute("markdown", value);
                case salesText -> model.addAttribute("sales", value);
            }
        });
        return "index";
    }
}
