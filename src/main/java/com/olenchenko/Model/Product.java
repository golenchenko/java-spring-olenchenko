package com.olenchenko.Model;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.util.HashMap;
import java.util.Objects;
@Entity
@Table(name = "products")
@NoArgsConstructor
@Getter
@Setter
public class Product extends ProductCard {
    @Column(columnDefinition = "TEXT")
    private String description;
    @Type(JsonType.class)
    @Column(columnDefinition = "json")
    private HashMap<String, String> properties;

    @Override
    public String toString() {
        return "Product{" +
                "description='" + description + '\'' +
                ", properties=" + properties +
                "} " + super.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Product product = (Product) o;
        return Objects.equals(getDescription(), product.getDescription()) &&
                Objects.equals(getProperties(), product.getProperties());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDescription(), getProperties());
    }
}
