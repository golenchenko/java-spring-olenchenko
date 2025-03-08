package com.olenchenko.parser.utils;

import java.util.HashMap;

public class Utils {
    public Utils() {
    }

    public String formatVariationTitle(String title) {
        return title.split("\\|")[1].replace(" :", "");
    }
    public String hashMapFiltersToUrlQuery(HashMap<String, String> filters) {
        StringBuilder query = new StringBuilder();
        for (String key : filters.keySet()) {
            query.append("&").append(key).append("=").append(filters.get(key));
        }
        return query.toString();
    }
}
