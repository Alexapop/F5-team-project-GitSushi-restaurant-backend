package dev.team1.products;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.team1.contracts.IProductService;
import dev.team1.enums.ProductCategory;
import dev.team1.products.dtos.ProductDTOResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


@RestController 
@RequestMapping(path = "${api-endpoint}/products")
public class ProductController {

    private final IProductService productsService;

    public ProductController(IProductService productsService) {
        this.productsService = productsService;
    }

    @GetMapping("")
    public ResponseEntity<Page<ProductDTOResponse>> index(
        @RequestParam(required = false) ProductCategory category,
        Pageable pageable
    ) {
        if (category != null) {
            return ResponseEntity.ok(
                productsService.getByCategory(category, pageable)
            );
        }
        
        return ResponseEntity.ok(
            productsService.getAllAvailable(pageable)
        );
    }

    @GetMapping("administration")
    public ResponseEntity<Page<ProductDTOResponse>> administration(Pageable pageable) {
        return ResponseEntity.ok(
            productsService.getAll(pageable)
        );
    }
    

    @GetMapping("{id}")
    public ResponseEntity<ProductDTOResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
            productsService.getById(id)
        );
    }

}
