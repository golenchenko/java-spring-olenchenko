package com.olenchenko.parser;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Getter
@Setter
public class TouchParser {
    private final static String url = "https://touch.com.ua/ua/";
    private final static String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36";
    private Document mainPage;
    private List<HashMap<String, Object>> newProducts;

    public TouchParser() {
        try {
            mainPage = Jsoup.connect(url).userAgent(userAgent).get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public List<HashMap<String, Object>> getNewProducts(boolean force) {
        if (newProducts == null || force) {
            newProducts = this.parseNewProducts();
        }
        return newProducts;
    }
    public String getUrl() {
        return url;
    }
    public List<HashMap<String, Object>> getNewProducts() {
        return getNewProducts(false);
    }
    private String formatUrl(String url) {
        return this.getUrl() + url.replaceFirst("/", "");
    }
    private String formatVariationTitle(String title) {
        return title.split("\\|")[1].replace(" :", "");
    }
    private List<HashMap<String, Object>> parseNewProducts() {
        Document mainPage = this.getMainPage();
        Element homeCatalog = mainPage.getElementById("homeCatalog");
        List<HashMap<String, Object>> newProductsList = new ArrayList<>();
        if (homeCatalog != null) {
            for(Element product : homeCatalog.getElementsByAttributeValue("id", "sliderBlock_productList")) {
               String titleOfTheBlock = product.getElementsByTag("div").getFirst().getElementsByTag("h2").text().strip();
               switch (titleOfTheBlock) {
                   case "Новинка":

                       Elements products = product.selectXpath("//*[@class=\"items productList swiper-wrapper\"]");
                       HashMap<String, Object> newProduct;
                       for(Element productBlock : products) {
                           newProduct = new HashMap<>();
                            Element productElement = productBlock.getElementsByTag("div").getFirst();
                            Element tabloid = productElement.getElementsByClass("tabloid").getFirst();

                            String url = tabloid.select("a.name").attr("href");
                            String imageUrl = tabloid.select("a.picture").select("img").attr("src");
                            String title = tabloid.select("a.name").text().strip();
                            String article = tabloid.select("a.picture").select("div.article > span.artnum_span > span.changeArticle").text();
                            Element anotherVariations;
                            if (!tabloid.getElementsByClass("skuProperty").isEmpty()) {
                                HashMap<String, Object> variations = new HashMap<>();
                                anotherVariations = tabloid.getElementsByClass("skuProperty").getFirst();
                                List<HashMap<String, String>> skuPropertyList = new ArrayList<>();
                                HashMap<String, String> skuProperty;
                                String typeOfAnotherVariations = formatVariationTitle(tabloid.getElementsByClass("skuProperty").getFirst().getElementsByClass("skuPropertyName").getFirst().text());
                                for(Element sku : anotherVariations.select("div.skuProperty > ul.skuPropertyList > li.skuPropertyValue")){
                                    skuProperty = new HashMap<>();

                                    Element skuData = sku.select("div.bg_border").getFirst();

                                    String skuName = skuData.select("a.elementSkuPropertyLink").attr("title");
                                    String skuDataId = skuData.select("a.elementSkuPropertyLink").attr("data-id");
                                    String skuUrl = skuData.select("a.elementSkuPropertyLink").attr("href");
                                    skuProperty.put("title", skuName);
                                    skuProperty.put("dataId", skuDataId);
                                    skuProperty.put("url", formatUrl(skuUrl));
                                    skuPropertyList.add(skuProperty);
                                }
                                variations.put(typeOfAnotherVariations, skuPropertyList);
                                newProduct.put("variations", variations);
                            }
                           newProduct.put("url", formatUrl(url));
                            newProduct.put("imageUrl", formatUrl(imageUrl));
                            newProduct.put("title", title);
                            newProduct.put("article", article);

                            newProductsList.add(newProduct);





                       }
               }
            }
            return newProductsList;
        }
        return null;
    }
}
