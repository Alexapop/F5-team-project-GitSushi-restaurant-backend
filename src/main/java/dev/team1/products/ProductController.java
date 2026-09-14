package dev.team1.products;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.team1.contracts.IProductService;
import dev.team1.enums.ProductCategory;
import dev.team1.products.dtos.ProductAvailableDTO;
import dev.team1.products.dtos.ProductDTORequest;
import dev.team1.products.dtos.ProductDTOResponse;
import dev.team1.products.dtos.ProductExclusiveDTO;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



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


    @PostMapping("")
    public ResponseEntity<ProductDTOResponse> store(@Valid @RequestBody ProductDTORequest dto) {
        return ResponseEntity.status(201).body(
            productsService.store(dto)
        );
    }

    @PatchMapping("{id}")
    public ResponseEntity<ProductDTOResponse> update(@PathVariable Long id, @Valid @RequestBody ProductDTORequest dto) {
        return ResponseEntity.ok(
            productsService.update(id, dto)
        );
    }

    @PatchMapping("{id}/availability")
    public ResponseEntity<Void> updateAvailability(@PathVariable Long id, @Valid @RequestBody ProductAvailableDTO dto) {
        productsService.updadeAvailability(id, dto);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("{id}/exclusive")
    public ResponseEntity<Void> updateExclusive(@PathVariable Long id, @Valid @RequestBody ProductExclusiveDTO dto) {
        productsService.updadeExclusive(id, dto);
        return ResponseEntity.noContent().build();
    }
    

}
