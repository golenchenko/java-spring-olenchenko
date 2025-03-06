package com.olenchenko.controllers.frontend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Controller
public class PagesController {
    private final String apiUrl;
    public PagesController(@Value("${api}") String apiUrl) {
        this.apiUrl = apiUrl;
    }

    @GetMapping("/")
    public String index(Model model) {
        // Get data from ApiController about new products
        String url = apiUrl + "newproducts";
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );
        List<Map<String, Object>> products = response.getBody();
        model.addAttribute("products", products);
        return "index";
    }
}
