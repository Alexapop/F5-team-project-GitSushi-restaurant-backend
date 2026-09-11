package dev.team1.products;

import org.springframework.data.domain.Pageable;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import dev.team1.enums.ProductCategory;

public interface ProductRepository extends JpaRepository<ProductEntity, Long>{

    public Page<ProductEntity> findByAvailableIsTrue(Pageable pageable);

    public Page<ProductEntity> findAll(Pageable pageable);

    @Query("select p from ProductEntity p where p.category = ?1 and p.available = TRUE")
    public Page<ProductEntity> findByCategory(ProductCategory category, Pageable pegeable);

}
