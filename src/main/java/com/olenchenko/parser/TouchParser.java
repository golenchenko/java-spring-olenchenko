package com.olenchenko.parser;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.olenchenko.Model.Product;
import com.olenchenko.Model.ProductCard;
import com.olenchenko.parser.utils.Utils;
import lombok.Getter;
import lombok.Setter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.olenchenko.Constants.*;

@Component
public class TouchParser {
    @Getter
    private final static String url = "https://touch.com.ua/";


    private final static String languageTag = "ua";

    private final static String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36";

    @Setter
    private List<ProductCard> newProducts;
    @Setter
    private List<ProductCard> bestSellers;
    // markdown - уцінка
    @Setter
    private List<ProductCard> markdown;
    @Setter
    private List<ProductCard> sales;

    private Document exchangeRateJson;

    @Setter
    @Getter
    private Document mainPage;
    private boolean needToRefresh = false;
    private final Utils utils = new Utils();

    public List<ProductCard> getSales() {
        if (sales == null || needToRefresh) {
            parseMainPage();
        }
        return sales;
    }

    public List<ProductCard> getMarkdown() {
        if (markdown == null || needToRefresh) {
            parseMainPage();
        }
        return markdown;
    }

    public List<ProductCard> getBestSellers() {
        if (bestSellers == null || needToRefresh) {
            parseMainPage();
        }
        return bestSellers;
    }

    public TouchParser() {
        refreshMainPage();
    }

    public void refreshMainPage() {
        try {
            setMainPage(Jsoup.connect(getUrl() + languageTag).userAgent(userAgent).followRedirects(true).get());
            needToRefresh = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ProductCard> getNewProducts() {
        if (newProducts == null || needToRefresh) {
            parseMainPage();
        }
        return newProducts;
    }

    private String formatUrl(String url) {
        if (url.contains("/" + languageTag + "/")) {
            return formatUrl(url, true);
        }
        if (url.contains("https://")) {
            return url.replace(getUrl(), getUrl() + languageTag + "/");
        }
        return getUrl() + languageTag + "/" + url.replaceFirst("/", "");
    }

    private String formatUrl(String url, boolean withoutLanguageTag) {
        if (withoutLanguageTag) {
            return getUrl() + url.replaceFirst("/", "");
        }
        return formatUrl(url);
    }

    private String getHQImageUrl(String url) {
        url = formatUrl(url, true);
        url = url.replace("webp/resize_cache/", "webp/");
        return url.replaceAll("\\d+_\\d+_\\d+/", "");
    }

    private double convertPriceFromUah(String price, int currencyCode) {
        if (price.isEmpty()) {
            return 0.0;
        }
        double priceInDouble = utils.parseStringAsDouble(price);
        try {
            if (exchangeRateJson == null) {
                exchangeRateJson = Jsoup.connect(exchangeApiUrl).userAgent(userAgent).ignoreContentType(true).get();
            }
            JsonArray jsonArray = JsonParser.parseString(exchangeRateJson.text()).getAsJsonArray();
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
                if (jsonObject.get("currencyCodeA").getAsInt() == currencyCode && jsonObject.get("currencyCodeB").getAsInt() == UAH_CODE) {
                    priceInDouble = priceInDouble / jsonObject.get("rateSell").getAsDouble();
                    return priceInDouble;
                }
            }
            return 0.0;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ProductCard getDataFromProductCard(Element element) {
        ProductCard productCard = new ProductCard();
        Element tabloid = element.getElementsByClass("tabloid").getFirst();

        String url = tabloid.select("a.name").attr("href");
        Elements image = tabloid.select("a.picture").select("img");
        String imageUrl;
        if (!image.attr("data-src").isEmpty()) {
            imageUrl = image.attr("data-src");
        } else {
            imageUrl = image.attr("src");
        }
        String title = tabloid.select("a.name").text().strip();
        String article = tabloid.select("a.picture").select("div.article > span.artnum_span > span.changeArticle").text();

        productCard.setTitle(title);
        productCard.setImageUrl(getHQImageUrl(imageUrl));
        productCard.setUrl(formatUrl(url));
        productCard.setArticle(Integer.parseInt(article));

        Element forPrice = tabloid.select("div.buy_data").getFirst();
        String priceWithoutDiscount = forPrice.select("div.old_price > span.discount").text();
        priceWithoutDiscount = priceWithoutDiscount.isEmpty()? "0.0" : priceWithoutDiscount;

        String priceWithDiscount = forPrice.select("a.price > div:nth-child(2)").text();

        if (!priceWithDiscount.matches(".*\\d.*")) {
            Element temp = forPrice;
            temp.select("div.old_price").remove();
            priceWithDiscount = temp.select("a.price").text();
            priceWithDiscount = priceWithDiscount.isEmpty() ? "0.0" : priceWithDiscount;
        }

        productCard.setPriceWithoutDiscount(utils.parseStringAsDouble(priceWithoutDiscount));
        productCard.setPriceWithDiscount(utils.parseStringAsDouble(priceWithDiscount));
        productCard.setPriceInUSDWithDiscount(convertPriceFromUah(priceWithDiscount, USD_CODE));
        productCard.setPriceInUSDWithoutDiscount(convertPriceFromUah(priceWithoutDiscount, USD_CODE));

        if (!tabloid.getElementsByClass("skuProperty").isEmpty()) {
            Element anotherVariations = tabloid.getElementsByClass("skuProperty").getFirst();
            if (!anotherVariations.select("div.skuProperty > ul.skuPropertyList > li.skuPropertyValue > div.bg_border").isEmpty()) {
// TODO: use Model for variations
                HashMap<String, List<HashMap<String, String>>> variations = new HashMap<>();
                List<HashMap<String, String>> skuPropertyList = new ArrayList<>();
                HashMap<String, String> skuProperty;
                String typeOfAnotherVariations = utils.formatVariationTitle(tabloid.getElementsByClass("skuProperty").getFirst().getElementsByClass("skuPropertyName").getFirst().text());
                for (Element sku : anotherVariations.select("div.skuProperty > ul.skuPropertyList > li.skuPropertyValue")) {
                    Element skuData = sku.select("div.bg_border").getFirst();
                    skuProperty = new HashMap<>();
                    String skuName = skuData.select("a.elementSkuPropertyLink").attr("title");
//                    String skuDataId = skuData.select("a.elementSkuPropertyLink").attr("data-id");
                    String skuUrl = skuData.select("a.elementSkuPropertyLink").attr("href");
                    skuProperty.put("title", skuName);
//                    skuProperty.put("dataId", skuDataId);
                    skuProperty.put("url", formatUrl(skuUrl));
                    skuPropertyList.add(skuProperty);
                }
                variations.put(typeOfAnotherVariations, skuPropertyList);
                productCard.setVariations(variations);
            }

        }
        return productCard;
    }

    private HashMap<String, List<HashMap<String, String>>> getVariantsFromProductPage(Element productPage) {
        HashMap<String, List<HashMap<String, String>>> variants = new HashMap<>();
        Elements variantsBlock = productPage.getElementsByClass("elementSkuProperty");
        for (Element skuProperty : variantsBlock) {
            if (!skuProperty.getElementsByClass("product_condition").isEmpty()) {
//                String skuName;
//                String skuUrl;
//                Elements skuList = skuProperty.select("div.product_condition > div.product_condition_wrap");
//                for (Element sku : skuList) {
//                    skuUrl = sku.getElementsByClass("elementSkuPropertyLink").attr("href");
//                    skuPropertyHashMap.put("title", skuName);
//                    skuPropertyHashMap.put("url", formatUrl(skuUrl));
//
//                }
                continue;
            }


            String title = utils.formatVariationTitle(skuProperty.getElementsByClass("elementSkuPropertyName").text());

            List<HashMap<String, String>> skuPropertyList = new ArrayList<>();
            HashMap<String, String> skuPropertyHashMap;
            Element skuPropertyListElement = skuProperty.getElementsByClass("elementSkuPropertyList").getFirst();
            Elements skuPropertyListElements = skuPropertyListElement.getElementsByClass("elementSkuPropertyValue");
            for (Element sku : skuPropertyListElements) {
                skuPropertyHashMap = new HashMap<>();
                Elements div = sku.select("div.bg_border");
                String skuName;
                String skuUrl;
                if (div.isEmpty()) {
                    skuName = sku.getElementsByClass("elementSkuPropertyLink").attr("title");
                    skuUrl = sku.getElementsByClass("elementSkuPropertyLink").attr("href");
                } else {
                    Element bgBorderDiv = div.getFirst();
                    skuName = bgBorderDiv.getElementsByClass("elementSkuPropertyLink").attr("title");
                    skuUrl = bgBorderDiv.getElementsByClass("elementSkuPropertyLink").attr("href");
                }
                skuPropertyHashMap.put("title", skuName);
                skuPropertyHashMap.put("url", formatUrl(skuUrl));
                skuPropertyList.add(skuPropertyHashMap);
            }
            variants.put(title, skuPropertyList);
        }
        return variants;
    }

    private List<ProductCard> getProductsFromCarousel(Element product) {
        List<ProductCard> productsList = new ArrayList<>();
        Elements products = product.select(".item.product.sku.swiper-slide");
        for (Element productBlock : products) {
            productsList.add(getDataFromProductCard(productBlock));
        }
        return productsList;
    }

    public void parseMainPage() {
        Document mainPage = getMainPage();
        Element homeCatalog = mainPage.getElementById("homeCatalog");
        if (homeCatalog != null) {
            Elements blocks = homeCatalog.getElementsByAttributeValue("id", "sliderBlock_productList");
            for (Element product : blocks) {
                String titleOfTheBlock = product.getElementsByTag("div").getFirst().getElementsByTag("h2").text().strip();
                List<ProductCard> productsList = getProductsFromCarousel(product);
                switch (titleOfTheBlock) {
                    case newProductsText:
                        setNewProducts(productsList);
                        break;
                    case bestSellersText:
                        setBestSellers(productsList);
                        break;
                    case salesText:
                        setSales(productsList);
                        break;
                    case markdownText:
                        setMarkdown(productsList);
                        break;
                }
            }
            needToRefresh = false;
        }
    }

    public HashMap<String, List<ProductCard>> getMergedCategories() {
        if (newProducts == null || bestSellers == null || sales == null || markdown == null) {
            parseMainPage();
        }
        HashMap<String, List<ProductCard>> mergedCategories = new HashMap<>();
        mergedCategories.put(newProductsText, getNewProducts());
        mergedCategories.put(bestSellersText, getBestSellers());
        mergedCategories.put(salesText, getSales());
        mergedCategories.put(markdownText, getMarkdown());
        return mergedCategories;
    }

    private Product getDataFromProductPage(Element element) {
        Product product = new Product();
        String url = element.select("link[rel=canonical]").attr("href");
        int article = Integer.parseInt(element.getElementsByClass("changeArticle").getFirst().text());
        String imageUrl = getHQImageUrl(element.select("meta[property=og:image]").attr("content"));
        String description = element.getElementsByClass("changeDescription").getFirst().text();
        String title = element.getElementsByClass("changeName").getFirst().text();
        String priceWithDiscount = element.getElementsByClass("changePrice").getFirst().text();
        priceWithDiscount = priceWithDiscount.isEmpty() ? "0.0" : priceWithDiscount;

        String priceWithoutDiscount = "";
        int priceWithoutDiscountElement = element.getElementsByClass("old_new_price").size();
        if (priceWithoutDiscountElement > 0) {
            priceWithoutDiscount = element.getElementsByClass("old_new_price").getFirst().text();
            priceWithoutDiscount = priceWithoutDiscount.isEmpty()? "0.0" : priceWithoutDiscount;
        }

        Element properties = element.getElementsByClass("wd_propsorter").getFirst();
        HashMap<String, String> productProperties = new HashMap<>();

        Element tableOfProperties = properties.getElementsByTag("tbody").getFirst();
        tableOfProperties.select("tr.row_empty").remove();
        tableOfProperties.select("tr.row_header").remove();
        for (Element property : tableOfProperties.getElementsByTag("tr")) {
            String key = property.getElementsByClass("cell_name").getFirst().text();
            String value = property.getElementsByClass("cell_value").getFirst().text();
            productProperties.put(key, value);
        }
        product.setDescription(description);
        product.setTitle(title);
        product.setUrl(url);
        product.setImageUrl(imageUrl);
        product.setArticle(article);
        product.setPriceWithDiscount(utils.parseStringAsDouble(priceWithDiscount));
        product.setPriceInUSDWithDiscount(convertPriceFromUah(priceWithDiscount, USD_CODE));
        product.setPriceWithoutDiscount(utils.parseStringAsDouble(priceWithoutDiscount));
        product.setPriceInUSDWithoutDiscount(convertPriceFromUah(priceWithoutDiscount, USD_CODE));
        product.setProperties(productProperties);

        HashMap<String, List<HashMap<String, String>>> variants = getVariantsFromProductPage(element);
        product.setVariations(variants);
        return product;
    }

    public HashMap<String, List<HashMap<String, String>>> getProductByUrl(String url) {
        return null;
    }

    public Product getProductByArticle(int id) {
        try {

            HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getUrl() +
                            languageTag +
                            "/search/?q=" +
                            URLEncoder.encode(String.valueOf(id), StandardCharsets.UTF_8)))
                    .GET()
                    .setHeader("user-agent", userAgent)
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String location = response.headers().firstValue("Location").get();
            if (!location.contains("/" + languageTag + "/")) {
                location = location.replace("/item/", "/" + languageTag + "/item/");
            }
            request = HttpRequest.newBuilder()
                    .uri(URI.create(location))
                    .GET()
                    .setHeader("user-agent", userAgent)
                    .build();
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return getDataFromProductPage(Jsoup.parse(response.body()));
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<ProductCard> getProductsFromQuery(String query, HashMap<String, String> filters) {
        List<ProductCard> products = new ArrayList<>();
        Document searchPage;
        try {

            HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(getUrl() +
                            languageTag +
                            "/search/?q=" +
                            URLEncoder.encode(query, StandardCharsets.UTF_8) +
                            utils.hashMapFiltersToUrlWithQuery(filters)))
                    .GET()
                    .setHeader("user-agent", userAgent)
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String location = "";
            if (response.headers().firstValue("Location").isPresent()) {
                location = formatUrl(response.headers().firstValue("Location").get());
                request = HttpRequest.newBuilder()
                        .uri(URI.create(location))
                        .GET()
                        .setHeader("user-agent", userAgent)
                        .build();
                response = client.send(request, HttpResponse.BodyHandlers.ofString());

            }
            searchPage = Jsoup.parse(response.body());

            if (!searchPage.select("#breadcrumbs").isEmpty()) {
                String url = URI.create(location + utils.hashMapToCategorizedUrl(filters)).toString();
                client = HttpClient.newHttpClient();
                request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .setHeader("user-agent", userAgent)
                        .build();

                response = client.send(request, HttpResponse.BodyHandlers.ofString());
                searchPage = Jsoup.parse(response.body());
            }
            if (searchPage.getElementsByClass("not_found_text").isEmpty() || searchPage.getElementsByClass("emptyWrapper").isEmpty()) {
                Elements resultPage = searchPage.getElementsByClass("items productList");
                Element resultPageElement;
                if (resultPage.isEmpty()) {
                    resultPageElement = searchPage.selectFirst("div.ajaxContainer > div");
                } else {
                    resultPageElement = resultPage.getFirst().getElementsByTag("div").getFirst();
                }
                Elements productsBlocks = resultPageElement.select(".item.product.sku");

                for (Element product : productsBlocks) {
                    if (!product.getElementsByTag("noindex").isEmpty()) {
                        continue;
                    }
                    products.add(getDataFromProductCard(product));
                }
            }
            return products;
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }

    }
}
