package com.olenchenko.parser;

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
    @Setter
    private List<ProductCard> markdown; // markdown - уцінка
    @Setter
    private List<ProductCard> sales;
    @Setter
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
        try {
            setExchangeRateJson(Jsoup.connect(exchangeApiUrl).userAgent(userAgent).ignoreContentType(true).get());
        } catch (IOException ignored) {
        }
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
    private double convertPriceFromUah(String price, String currencyCode) {
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
                if (jsonObject.get("ccy").getAsString().equals(currencyCode) && jsonObject.get("base_ccy").getAsString().equals(UAH_CODE)) {
                    priceInDouble = priceInDouble / jsonObject.get("sale").getAsDouble();
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
        Elements image = tabloid.select("a.picture").select("img");

        productCard.setTitle(tabloid.select("a.name").text().strip());
        productCard.setImageUrl(getHQImageUrl(!image.attr("data-src").isEmpty() ? image.attr("data-src") : image.attr("src")));
        productCard.setUrl(formatUrl(tabloid.select("a.name").attr("href")));
        productCard.setArticle(Integer.parseInt(tabloid.select("a.picture").select("div.article > span.artnum_span > span.changeArticle").text()));

        Element forPrice = tabloid.select("div.buy_data").getFirst();
        String priceWithoutDiscount = forPrice.select("div.old_price > span.discount").text();
        priceWithoutDiscount = priceWithoutDiscount.isEmpty()? "0.0" : priceWithoutDiscount;

        String priceWithDiscount = forPrice.select("a.price > div:nth-child(2)").text();

        if (!priceWithDiscount.matches(".*\\d.*")) {
            forPrice.select("div.old_price").remove();
            priceWithDiscount = forPrice.select("a.price").text();
            priceWithDiscount = priceWithDiscount.isEmpty() ? "0.0" : priceWithDiscount;
        }

        productCard.setPriceWithoutDiscount(utils.parseStringAsDouble(priceWithoutDiscount));
        productCard.setPriceWithDiscount(utils.parseStringAsDouble(priceWithDiscount));
        productCard.setPriceInUSDWithDiscount(convertPriceFromUah(priceWithDiscount, USD_CODE));
        productCard.setPriceInUSDWithoutDiscount(convertPriceFromUah(priceWithoutDiscount, USD_CODE));

        if (!tabloid.getElementsByClass("skuProperty").isEmpty()) {
            Element anotherVariations = tabloid.getElementsByClass("skuProperty").getFirst();
            if (!anotherVariations.select("div.skuProperty > ul.skuPropertyList > li.skuPropertyValue > div.bg_border").isEmpty()) {
                productCard.setVariations(getVariantsFromProductCard(tabloid));
            }
        }
        return productCard;
    }
    private HashMap<String, List<HashMap<String, String>>> getVariantsFromProductCard(Element tabloid) {
        HashMap<String, List<HashMap<String, String>>> variations = new HashMap<>();
        Element anotherVariations = tabloid.getElementsByClass("skuProperty").getFirst();
        if (!anotherVariations.select("div.skuProperty > ul.skuPropertyList > li.skuPropertyValue > div.bg_border").isEmpty()) {
            String typeOfAnotherVariations = utils.formatVariationTitle(tabloid.getElementsByClass("skuProperty").getFirst().getElementsByClass("skuPropertyName").getFirst().text());
            variations.put(typeOfAnotherVariations, getSkuPropertiesFromProductCard(tabloid));
        }
        return variations;
    }
    private List<HashMap<String, String>> getSkuPropertiesFromProductCard(Element tabloid) {
        List<HashMap<String, String>> skuPropertyList = new ArrayList<>();
        Element anotherVariations = tabloid.getElementsByClass("skuProperty").getFirst();
        HashMap<String, String> skuProperty;
        for (Element sku : anotherVariations.select("div.skuProperty > ul.skuPropertyList > li.skuPropertyValue")) {
            Element skuData = sku.select("div.bg_border").getFirst();
            skuProperty = new HashMap<>();
            String skuName = skuData.select("a.elementSkuPropertyLink").attr("title");
            String skuUrl = skuData.select("a.elementSkuPropertyLink").attr("href");
            skuProperty.put("title", skuName);
            skuProperty.put("url", formatUrl(skuUrl));
            skuPropertyList.add(skuProperty);
        }
        return skuPropertyList;
    }
    private HashMap<String, List<HashMap<String, String>>> getVariantsFromProductPage(Element productPage) {
        HashMap<String, List<HashMap<String, String>>> variants = new HashMap<>();
        Elements variantsBlock = productPage.getElementsByClass("elementSkuProperty");
        for (Element skuProperty : variantsBlock) {
            if (!skuProperty.getElementsByClass("product_condition").isEmpty()) {
                continue;
            }
            String title = utils.formatVariationTitle(skuProperty.getElementsByClass("elementSkuPropertyName").text());
            variants.put(title, getSkuPropertiesFromProductPage(skuProperty));
        }
        return variants;
    }
    private List<HashMap<String, String>> getSkuPropertiesFromProductPage(Element skuProperty) {
        List<HashMap<String, String>> skuPropertyList = new ArrayList<>();
        HashMap<String, String> skuPropertyHashMap;
        Element skuPropertyListElement = skuProperty.getElementsByClass("elementSkuPropertyList").getFirst();
        Elements skuPropertyListElements = skuPropertyListElement.getElementsByClass("elementSkuPropertyValue");
        for (Element sku : skuPropertyListElements) {
            skuPropertyHashMap = new HashMap<>();
            Elements div = sku.select("div.bg_border");
            boolean emptyDiv = div.isEmpty();

            String skuName = emptyDiv ? sku.getElementsByClass("elementSkuPropertyLink").attr("title") :
                    div.getFirst().getElementsByClass("elementSkuPropertyLink").attr("title");
            String skuUrl = emptyDiv ? sku.getElementsByClass("elementSkuPropertyLink").attr("href") :
                    div.getFirst().getElementsByClass("elementSkuPropertyLink").attr("href");
            skuPropertyHashMap.put("title", skuName);
            skuPropertyHashMap.put("url", formatUrl(skuUrl));
            skuPropertyList.add(skuPropertyHashMap);
        }
        return skuPropertyList;
    }
    private List<ProductCard> getProductsFromCarousel(Element product) {
        List<ProductCard> productsList = new ArrayList<>();
        Elements products = product.select(".item.product.sku.swiper-slide");
        for (Element productBlock : products) {
            productsList.add(getDataFromProductCard(productBlock));
        }
        return productsList;
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
    private HashMap<String, String> getProductProperties(Element element) {
        HashMap<String, String> productProperties = new HashMap<>();
        Element properties = element.getElementsByClass("wd_propsorter").getFirst();

        Element tableOfProperties = properties.getElementsByTag("tbody").getFirst();
        tableOfProperties.select("tr.row_empty").remove();
        tableOfProperties.select("tr.row_header").remove();
        for (Element property : tableOfProperties.getElementsByTag("tr")) {
            String key = property.getElementsByClass("cell_name").getFirst().text();
            String value = property.getElementsByClass("cell_value").getFirst().text();
            productProperties.put(key, value);
        }
        return productProperties;
    }
    private Product getDataFromProductPage(Element element) {
        Product product = new Product();
        product.setUrl(element.select("link[rel=canonical]").attr("href"));
        product.setDescription(element.getElementsByClass("changeDescription").getFirst().wholeText());
        product.setTitle(element.getElementsByClass("changeName").getFirst().text());
        product.setImageUrl(getHQImageUrl(element.select("meta[property=og:image]").attr("content")));
        product.setArticle(Integer.parseInt(element.getElementsByClass("changeArticle").getFirst().text()));

        String priceWithDiscount = element.getElementsByClass("changePrice").getFirst().text();
        priceWithDiscount = priceWithDiscount.isEmpty() ? "0.0" : priceWithDiscount;

        String priceWithoutDiscount = "";
        int priceWithoutDiscountElement = element.getElementsByClass("old_new_price").size();
        if (priceWithoutDiscountElement > 0) {
            priceWithoutDiscount = element.getElementsByClass("old_new_price").getFirst().text();
            priceWithoutDiscount = priceWithoutDiscount.isEmpty() ? "0.0" : priceWithoutDiscount;
        }

        product.setPriceWithDiscount(utils.parseStringAsDouble(priceWithDiscount));
        product.setPriceInUSDWithDiscount(convertPriceFromUah(priceWithDiscount, USD_CODE));
        product.setPriceWithoutDiscount(utils.parseStringAsDouble(priceWithoutDiscount));
        product.setPriceInUSDWithoutDiscount(convertPriceFromUah(priceWithoutDiscount, USD_CODE));
        product.setProperties(getProductProperties(element));
        product.setVariations(getVariantsFromProductPage(element));
        return product;
    }
    private HttpResponse<String> searchProduct(String url) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .setHeader("user-agent", userAgent)
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
    public Product getProductByArticle(int id) {
        try {
            HttpResponse<String> response = searchProduct(getUrl() +
                    languageTag +
                    "/search/?q=" +
                    URLEncoder.encode(String.valueOf(id), StandardCharsets.UTF_8));
            String location = response.headers().firstValue("Location").get();
            if (!location.contains("/" + languageTag + "/")) {
                location = location.replace("/item/", "/" + languageTag + "/item/");
            }
            response = searchProduct(location);
            return getDataFromProductPage(Jsoup.parse(response.body()));
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
    public List<ProductCard> getProductsFromQuery(String query, HashMap<String, String> filters) {
        List<ProductCard> products = new ArrayList<>();
        Document searchPage;
        try {
            HttpResponse<String> response = searchProduct(getUrl() +
                    languageTag +
                    "/search/?q=" +
                    URLEncoder.encode(query, StandardCharsets.UTF_8) +
                    utils.hashMapFiltersToUrlWithQuery(filters));

            String location = "";
            if (response.headers().firstValue("Location").isPresent()) {
                location = formatUrl(response.headers().firstValue("Location").get());
                response = searchProduct(location);
            }
            searchPage = Jsoup.parse(response.body());

            if (!searchPage.select("#breadcrumbs").isEmpty()) {
                response = searchProduct(location + utils.hashMapToCategorizedUrl(filters));
                searchPage = Jsoup.parse(response.body());
            }
            if (searchPage.getElementsByClass("not_found_text").isEmpty() || searchPage.getElementsByClass("emptyWrapper").isEmpty()) {
                Elements productsBlocks = getProductBlocksFromSearch(searchPage);
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
    private Elements getProductBlocksFromSearch(Document searchPage) {
        Elements resultPage = searchPage.getElementsByClass("items productList");
        Element resultPageElement;
        if (resultPage.isEmpty()) {
            resultPageElement = searchPage.selectFirst("div.ajaxContainer > div");
        } else {
            resultPageElement = resultPage.getFirst().getElementsByTag("div").getFirst();
        }
        return resultPageElement.select(".item.product.sku");
    }
}
