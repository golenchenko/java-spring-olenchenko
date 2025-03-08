package com.olenchenko.parser;

import com.olenchenko.parser.utils.Utils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.olenchenko.Constants.*;

@Component
public class TouchParser {
    private final static String url = "https://touch.com.ua/";

    private final static String languageTag = "ua";

    private final static String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36";

    private List<HashMap<String, Object>> newProducts;
    private List<HashMap<String, Object>> bestSellers;
    // markdown - уцінка
    private List<HashMap<String, Object>> markdown;
    private List<HashMap<String, Object>> sales;

    private Document mainPage;
    private boolean needToRefresh = false;
    private final Utils utils = new Utils();

    public List<HashMap<String, Object>> getSales() {
        if (sales == null || needToRefresh) {
            parseMainPage();
        }
        return sales;
    }

    public void setSales(List<HashMap<String, Object>> sales) {
        this.sales = sales;
    }

    public List<HashMap<String, Object>> getMarkdown() {
        if (markdown == null || needToRefresh) {
            parseMainPage();
        }
        return markdown;
    }

    public void setMarkdown(List<HashMap<String, Object>> markdown) {
        this.markdown = markdown;
    }

    public List<HashMap<String, Object>> getBestSellers() {
        if (bestSellers == null || needToRefresh) {
            parseMainPage();
        }
        return bestSellers;
    }

    public void setBestSellers(List<HashMap<String, Object>> bestSellers) {
        this.bestSellers = bestSellers;
    }

    public void setNewProducts(List<HashMap<String, Object>> newProducts) {
        this.newProducts = newProducts;
    }

    public Document getMainPage() {
        return mainPage;
    }

    public void setMainPage(Document mainPage) {
        this.mainPage = mainPage;
    }

    public TouchParser() {
        refreshMainPage();
    }
    public void refreshMainPage() {
        try {
            setMainPage(Jsoup.connect(getUrl() + languageTag).userAgent(userAgent).get());
            needToRefresh = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public String getUrl() {
        return url;
    }
    public List<HashMap<String, Object>> getNewProducts() {
        if (newProducts == null || needToRefresh) {
            parseMainPage();
        }
        return newProducts;
    }

    private String formatUrl(String url) {
        if (url.contains("/" + languageTag + "/")) {
            return formatUrl(url, true);
        }
        return getUrl() + languageTag + "/" + url.replaceFirst("/", "");
    }
    private String formatUrl(String url, boolean withoutLanguageTag) {
        if (withoutLanguageTag) {
            return this.getUrl() + url.replaceFirst("/", "");
        }
        return formatUrl(url);
    }

    private HashMap<String, Object> getDataFromProductCard(Element element) {
        HashMap<String, Object> newProduct = new HashMap<>();
        Element tabloid = element.getElementsByClass("tabloid").getFirst();

        String url = tabloid.select("a.name").attr("href");
        String imageUrl = tabloid.select("a.picture").select("img").attr("src");
        String title = tabloid.select("a.name").text().strip();
        String article = tabloid.select("a.picture").select("div.article > span.artnum_span > span.changeArticle").text();


        newProduct.put("title", title);
        newProduct.put("url", formatUrl(url));
        newProduct.put("article", article);
        newProduct.put("imageUrl", formatUrl(imageUrl, true));

        if (!tabloid.getElementsByClass("skuProperty").isEmpty()) {
            Element anotherVariations = tabloid.getElementsByClass("skuProperty").getFirst();
            if (!anotherVariations.select("div.skuProperty > ul.skuPropertyList > li.skuPropertyValue > div.bg_border").isEmpty()) {

                HashMap<String, Object> variations = new HashMap<>();
                List<HashMap<String, String>> skuPropertyList = new ArrayList<>();
                HashMap<String, String> skuProperty;
                String typeOfAnotherVariations = utils.formatVariationTitle(tabloid.getElementsByClass("skuProperty").getFirst().getElementsByClass("skuPropertyName").getFirst().text());
                for (Element sku : anotherVariations.select("div.skuProperty > ul.skuPropertyList > li.skuPropertyValue")) {
                    Element skuData = sku.select("div.bg_border").getFirst();
                    skuProperty = new HashMap<>();
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

        }
        return newProduct;
    }
    private List<HashMap<String, Object>> getProductsFromCarousel(Element product) {
        List<HashMap<String, Object>> productsList = new ArrayList<>();
        Elements products = product.select(".item.product.sku.swiper-slide");
        for (Element productBlock : products) {
            productsList.add(getDataFromProductCard(productBlock));
        }
        return productsList;
    }
    public void parseMainPage() {
        Document mainPage = this.getMainPage();
        Element homeCatalog = mainPage.getElementById("homeCatalog");
        if (homeCatalog != null) {
            Elements blocks = homeCatalog.getElementsByAttributeValue("id", "sliderBlock_productList");
            for(Element product : blocks) {
                String titleOfTheBlock = product.getElementsByTag("div").getFirst().getElementsByTag("h2").text().strip();
                List<HashMap<String, Object>> productsList = getProductsFromCarousel(product);
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
    public HashMap<String, List<HashMap<String, Object>>> getMergedCategories() {
        if (newProducts == null || bestSellers == null || sales == null || markdown == null) {
            parseMainPage();
        }
        HashMap<String, List<HashMap<String, Object>>> mergedCategories = new HashMap<>();
        mergedCategories.put(newProductsText, getNewProducts());
        mergedCategories.put(bestSellersText, getBestSellers());
        mergedCategories.put(salesText, getSales());
        mergedCategories.put(markdownText, getMarkdown());
        return mergedCategories;
    }


    public List<HashMap<String, Object>> getProductsFromQuery(String query, HashMap<String, String> filters) {
        List<HashMap<String, Object>> products = new ArrayList<>();
        Document searchPage;
        try {
            searchPage = Jsoup.connect(
                    getUrl() +
                            languageTag +
                            "/search/?q=" +
                            URLEncoder.encode(query, StandardCharsets.UTF_8) +
                            utils.hashMapFiltersToUrlQuery(filters)
            ).userAgent(userAgent).get();

            if (searchPage.getElementsByClass("not_found_text").isEmpty()) {
                Element resultPage = searchPage.getElementsByClass("items productList").getFirst().getElementsByTag("div").getFirst();
                Elements productsBlocks = resultPage.select(".item.product.sku");

                for (Element product : productsBlocks) {
                    if (!product.getElementsByTag("noindex").isEmpty()) {
                        continue;
                    }
                    products.add(getDataFromProductCard(product));
                }
            }
            return products;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

    }
}
