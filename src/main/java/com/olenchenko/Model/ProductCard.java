package com.olenchenko.Model;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "product_cards")
@Inheritance(strategy = InheritanceType.JOINED)
@NoArgsConstructor
@Getter
@Setter
public class ProductCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int article;
    private String title;
    private String imageUrl;
    private String url;
    private double priceWithDiscount;
    private double priceInUSDWithDiscount;
    private double priceWithoutDiscount;
    private double priceInUSDWithoutDiscount;
    @Type(JsonType.class)
    @Column(columnDefinition = "json")
    private HashMap<String, List<HashMap<String, String>>> variations;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductCard that = (ProductCard) o;
        return getArticle() == that.getArticle() &&
                Objects.equals(getTitle(), that.getTitle()) &&
                Objects.equals(getImageUrl(), that.getImageUrl()) &&
                Objects.equals(getUrl(), that.getUrl()) &&
                Objects.equals(getPriceWithDiscount(), that.getPriceWithDiscount()) &&
                Objects.equals(getPriceInUSDWithDiscount(), that.getPriceInUSDWithDiscount()) &&
                Objects.equals(getPriceWithoutDiscount(), that.getPriceWithoutDiscount()) &&
                Objects.equals(getPriceInUSDWithoutDiscount(), that.getPriceInUSDWithoutDiscount()) &&
                Objects.equals(getVariations(), that.getVariations());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTitle(), getImageUrl(), getUrl(), getPriceWithDiscount(), getPriceWithoutDiscount(), getArticle(), getVariations(), getPriceInUSDWithDiscount(), getPriceInUSDWithoutDiscount());
    }

    @Override
    public String toString() {
        return "ProductCard{" +
                "title='" + title + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", url='" + url + '\'' +
                ", priceWithDiscount='" + priceWithDiscount + '\'' +
                ", priceInUSDWithDiscount='" + priceInUSDWithDiscount + '\'' +
                ", priceWithoutDiscount='" + priceWithoutDiscount + '\'' +
                ", priceInUSDWithoutDiscount='" + priceInUSDWithoutDiscount + '\'' +
                ", article=" + article +
                ", variations=" + variations +
                '}';
    }
}
