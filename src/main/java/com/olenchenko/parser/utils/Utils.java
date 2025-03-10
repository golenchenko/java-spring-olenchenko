package com.olenchenko.parser.utils;

import java.util.HashMap;

public class Utils {
    public Utils() {
    }

    public String formatVariationTitle(String title) {
        return title.split("\\|")[1].replace(" :", "");
    }
    public String hashMapFiltersToUrlWithQuery(HashMap<String, String> filters) {
        StringBuilder query = new StringBuilder();
        for (String key : filters.keySet()) {
            query.append("&").append(key).append("=").append(filters.get(key));
        }
        return query.toString();
    }
    public String hashMapFiltersToUrlWithoutQuery(HashMap<String, String> filters) {
        String query = hashMapFiltersToUrlWithQuery(filters);
        return query.replaceFirst("&", "?");
    }
    public String hashMapToCategorizedUrl(HashMap<String, String> filters) {
        StringBuilder query = new StringBuilder();
        if (filters.containsKey("set_filter")) {
            query.append("filter");
        }
        if (filters.containsKey("arrFilterFilter_P1_MIN") && filters.containsKey("arrFilterFilter_P1_MAX")) {
            StringBuilder minMaxFilter = new StringBuilder().append("price-base-from-").append(filters.get("arrFilterFilter_P1_MIN"))
                    .append("-to-").append(filters.get("arrFilterFilter_P1_MAX"));
            query.append("/").append(minMaxFilter);
            filters.remove("arrFilterFilter_P1_MIN");
            filters.remove("arrFilterFilter_P1_MAX");
        }
        else {
            StringBuilder priceFilter = new StringBuilder();
            if (filters.containsKey("arrFilterFilter_P1_MAX")) {
                priceFilter.append("price-base-to-").append(filters.get("arrFilterFilter_P1_MAX"));
                query.append("/").append(priceFilter);
                filters.remove("arrFilterFilter_P1_MAX");
            } else if (filters.containsKey("arrFilterFilter_P1_MIN")) {
                priceFilter.append("price-base-from-").append(filters.get("arrFilterFilter_P1_MIN"));
                query.append("/").append(priceFilter);
                filters.remove("arrFilterFilter_P1_MIN");
            }
        }
        if (filters.containsKey("set_filter")) {
            query.append("/apply/");
            filters.remove("set_filter");
        }
        query.append(hashMapFiltersToUrlWithoutQuery(filters));
        return query.toString();
    }
}
