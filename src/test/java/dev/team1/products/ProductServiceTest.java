package dev.team1.products;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.team1.enums.ProductCategory;
import dev.team1.products.dtos.ProductDTOResponse;
import dev.team1.products.exceptions.ProductExceptionNotFound;

@ExtendWith (MockitoExtension.class)
public class ProductServiceTest {
    
    @InjectMocks 
    ProductService service;

    @Mock 
    ProductRepository repository;

    private List<ProductEntity> sampleEntities;
    private List<ProductDTOResponse> sampleDTOs;

    @BeforeEach 
    void setup() {
        service = new ProductService(repository);
        
        sampleEntities = ProductTestData.sampleEntities();
        sampleDTOs = ProductTestData.sampleDTOs();
    }

    @Test 
    void testGetAll() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<ProductEntity> mockPage = new PageImpl<>(sampleEntities, pageable, sampleEntities.size());
        Page<ProductDTOResponse> mockPageDTO = new PageImpl<>(sampleDTOs, pageable, sampleDTOs.size());

        when(repository.findAll(pageable)).thenReturn(mockPage);
        Page<ProductDTOResponse> pageDTO = service.getAll(pageable);

        assertThat(pageDTO.getTotalElements(), is(equalTo(8L)));
        assertThat(pageDTO.getContent().get(0).available(), is(equalTo(true)));
        assertThat(pageDTO.getContent().get(1).available(), is(equalTo(false)));
        assertThat(pageDTO.getContent().get(3).name(), is(equalTo("Java Roll")));
        assertThat(pageDTO.getContent().get(5), is(equalTo(mockPageDTO.getContent().get(5))));
    }

    @Test 
    void testGetAllAvailable() {
        List<ProductEntity> availableEntities = sampleEntities.stream()
            .filter(p -> p.isAvailable())
            .collect(Collectors.toList());
        
        List<ProductDTOResponse> availableDTOs = sampleDTOs.stream()
            .filter(p -> p.available())
            .collect(Collectors.toList());
    
        Pageable pageable = PageRequest.of(0, 20);
        Page<ProductEntity> mockPage = new PageImpl<>(availableEntities, pageable, availableEntities.size());
        Page<ProductDTOResponse> mockPageDTO = new PageImpl<>(availableDTOs, pageable, availableDTOs.size());

        when(repository.findByAvailableIsTrue(pageable)).thenReturn(mockPage);
        Page<ProductDTOResponse> pageDTO = service.getAllAvailable(pageable);

        assertThat(pageDTO.getTotalElements(), is(equalTo(7L)));
        assertThat(pageDTO.getContent().get(0).available(), is(equalTo(true)));
        assertThat(pageDTO.getContent().get(1).available(), is(equalTo(true)));
        assertThat(pageDTO.getContent().get(2).name(), is(equalTo("Java Roll")));
        assertThat(pageDTO.getContent().get(5), is(equalTo(mockPageDTO.getContent().get(5))));
    }

    @Test 
    void testGetByCategory() {
        List<ProductEntity> availableEntities = sampleEntities.stream()
            .filter(p -> p.isAvailable() && p.getCategory() == ProductCategory.POSTRES)
            .collect(Collectors.toList());
        
        List<ProductDTOResponse> availableDTOs = sampleDTOs.stream()
            .filter(p -> p.available() && p.category() == ProductCategory.POSTRES)
            .collect(Collectors.toList());
    
        Pageable pageable = PageRequest.of(0, 20);
        Page<ProductEntity> mockPage = new PageImpl<>(availableEntities, pageable, availableEntities.size());
        Page<ProductDTOResponse> mockPageDTO = new PageImpl<>(availableDTOs, pageable, availableDTOs.size());

        when(repository.findByCategory(ProductCategory.POSTRES, pageable)).thenReturn(mockPage);
        Page<ProductDTOResponse> pageDTO = service.getByCategory(ProductCategory.POSTRES, pageable);

        assertThat(pageDTO.getTotalElements(), is(equalTo(2L)));
        assertThat(pageDTO.getContent().get(0).available(), is(equalTo(true)));
        assertThat(pageDTO.getContent().get(1).available(), is(equalTo(true)));
        assertThat(pageDTO.getContent().get(0).name(), is(equalTo("Mochi Python")));
        assertThat(pageDTO.getContent().get(1), is(equalTo(mockPageDTO.getContent().get(1))));
    }

    @Test 
    void testGetById() {
        ProductEntity mockItem = sampleEntities.get(0);
        ProductDTOResponse mockDTO = sampleDTOs.get(0);

        when(repository.findById(1L)).thenReturn(Optional.of(mockItem));
        ProductDTOResponse dto = service.getById(1L);

        assertThat(dto.available(), is(equalTo(true)));
        assertThat(dto.name(), is(equalTo("Ramen Fix")));
        assertThat(dto.name(), is(equalTo(mockDTO.name())));
    }

    @Test 
    void testGetByIdNotFound() {
        Long missingId = 500L;
        when(repository.findById(missingId)).thenReturn(Optional.empty());

        ProductExceptionNotFound exc = assertThrows(
            ProductExceptionNotFound.class, 
            () -> service.getById(missingId)
        );

        assertThat(exc.getMessage(), is(equalTo(
            "Cannot find product with id " + missingId + " because it doesn't exist."
        )));

    }

}
