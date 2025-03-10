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

import static com.olenchenko.Constants.*;

@Controller
public class PagesController {
    private final String apiUrl;
    public PagesController(@Value("${api}") String apiUrl) {
        this.apiUrl = apiUrl;
    }
    // The given ParameterizedTypeReference is used to pass generic type information.
    // https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/client/RestTemplate.html#exchange(java.net.URI,org.springframework.http.HttpMethod,org.springframework.http.HttpEntity,org.springframework.core.ParameterizedTypeReference)
    @GetMapping("/")
    public String index(Model model) {
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
    @GetMapping("/search")
    public String search(Model model) {
        model.addAttribute("apiUrl", apiUrl);
        return "search";
    }
}
