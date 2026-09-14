package dev.team1.products;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.team1.contracts.IProductService;
import dev.team1.enums.ProductCategory;
import dev.team1.mappers.ProductMapper;
import dev.team1.products.dtos.ProductDTOResponse;
import dev.team1.products.exceptions.ProductExceptionNotFound;

@Service 
public class ProductService implements IProductService {

    private final ProductRepository productsRepository;

    public ProductService(ProductRepository productsRepository) {
        this.productsRepository = productsRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDTOResponse> getAll(Pageable pageable) {
        Page<ProductEntity> pageEntity = productsRepository.findAll(pageable);
        
        return pageEntity.map(ProductMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDTOResponse> getAllAvailable(Pageable pageable) {
        Page<ProductEntity> pageEntity = productsRepository.findByAvailableIsTrue(pageable);
        
        return pageEntity.map(ProductMapper::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDTOResponse getById(Long id) {
        ProductEntity product = productsRepository.findById(id)
            .orElseThrow(() -> new ProductExceptionNotFound(
                "Cannot find product with id " + id + " because it doesn't exist."
            ));
        
        return ProductMapper.toDTO(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDTOResponse> getByCategory(ProductCategory category, Pageable pageable) {
        Page<ProductEntity> pageEntity = productsRepository.findByCategory(category, pageable);
        
        return pageEntity.map(ProductMapper::toDTO);
    }

}
