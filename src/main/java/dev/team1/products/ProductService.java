package dev.team1.products;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import dev.team1.contracts.IProductService;
import dev.team1.products.dtos.ProductAvailableDTO;
import dev.team1.enums.ProductCategory;
import dev.team1.mappers.ProductMapper;
import dev.team1.products.dtos.ProductDTORequest;
import dev.team1.products.dtos.ProductDTOResponse;
import dev.team1.products.dtos.ProductExclusiveDTO;
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

    @Override
    public ProductDTOResponse store(ProductDTORequest requestDTO) {
        if (productsRepository.existsByName(requestDTO.name())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Product already exists.");
        }
        
        ProductEntity entity = ProductMapper.toEntity(requestDTO);

        ProductEntity savedEntity = productsRepository.save(entity);

        return ProductMapper.toDTO(savedEntity);
    }

    @Override
    public ProductDTOResponse update(Long id, ProductDTORequest requestDTO) {
        ProductEntity originalEntity = productsRepository.findById(id)
            .orElseThrow(() -> new ProductExceptionNotFound(
                "Product " + id + " is not found"
            ));

        ProductEntity updated = ProductMapper.updateEntity(originalEntity, requestDTO);
        ProductEntity saved = productsRepository.save(updated);
        return ProductMapper.toDTO(saved);
    }

    @Override
    public void updadeAvailability(Long id, ProductAvailableDTO dto) {
        ProductEntity entity = productsRepository.findById(id)
            .orElseThrow(() -> new ProductExceptionNotFound(
                "Product " + id + " is not found"
            ));

        entity.setAvailable(dto.available());
        productsRepository.save(entity);
    }

    @Override
    public void updadeExclusive(Long id, ProductExclusiveDTO dto) {
        ProductEntity entity = productsRepository.findById(id)
            .orElseThrow(() -> new ProductExceptionNotFound(
                "Product " + id + " is not found"
            ));

        entity.setExclusive(dto.exclusive());
        productsRepository.save(entity);
    }


}
