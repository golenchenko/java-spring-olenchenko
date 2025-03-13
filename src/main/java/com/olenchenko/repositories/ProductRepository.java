package com.olenchenko.repositories;

import com.olenchenko.Model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Product findByArticle(int article);
}
