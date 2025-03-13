## Touch Parser

### General information

Data from the main page is **caching** after first request. If you want to refresh it, use api for refreshing this data
or open main page with **"refresh=true"** query.

### Available pages

`/` - main page  
`/search` - search page

### Available apis

`/api/newproducts` - returns all new products from main page  
`/api/sales` - returns all sales products from main page  
`/api/markdown` - returns all markdown products from main page  
`/api/bestsellers` - returns all bestsellers products from main page  
`/api/mergedcategories` - return merged categories products (from newproducts, sales, markdown, bestsellers
categories)  
`/api/refreshdata` - refresh all data from main page  
`/api/productdata/{id}` - returns product data by article id

```json
{
  "description": "text",
  "properties": {
    "property_1": "value_1",
    "property_2": "value_2",
    "property_3": "value_3"
  },
  "title": "text",
  "imageUrl": "text",
  "url": "text",
  "priceWithDiscount": "text",
  "priceWithoutDiscount": "text",
  "article": "number",
  "variations": {
    "variation_type_1": [
      {
        "title": "text",
        "url": "text"
      },
      {
        "title": "text_2",
        "url": "text_2"
      }
    ],
    "variation_type_2": [
      {
        "title": "text",
        "url": "text"
      },
      {
        "title": "text_2",
        "url": "text_2"
      }
    ]
  }
}
```

`/api/downloaddata/{id}` - download product data in XLSX format by article id  
`/api/search` - searching products by query. Available query params:

- `q` - Text for search
- `page_number` - Page number
- `sort_field` - Type of sorting ("SHOWS", "PRICE_ASC", "PRICE_DESC", "DATE")
- `min_price` - Minimum price of product
- `max_price` - Maximum price of product

```text
Response is json array with products like from /api/productdata/{id}
```
