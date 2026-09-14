package dev.team1.contracts;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import dev.team1.enums.ProductCategory;
import dev.team1.products.dtos.ProductDTORequest;
import dev.team1.products.dtos.ProductDTOResponse;

public interface IProductService extends IGenericGetService<ProductDTOResponse>, IGenericEditService<ProductDTORequest, ProductDTOResponse> {

    public Page<ProductDTOResponse> getByCategory(ProductCategory category, Pageable pageable);
    
    public Page<ProductDTOResponse> getAllAvailable(Pageable pageable);

    public void updadeAvailability(ProductAvailableDTO dto);

}
