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
import dev.team1.mappers.ProductMapper;
import dev.team1.products.dtos.ProductDTOResponse;
import dev.team1.products.exceptions.ProductExceptionNotFound;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import dev.team1.products.dtos.ProductDTOPatchRequest;
import dev.team1.products.dtos.ProductDTORequest;
import dev.team1.products.exceptions.ProductExceptionConflict;

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

    @Test
    void testStore_shouldStoreTheProduct() {
        ProductDTORequest mockReqDTO = ProductTestData.samplePOSTRequestDTO();
        ProductEntity savedEntity = sampleEntities.get(0);
        ProductDTOResponse expectedDTO = sampleDTOs.get(0);

        when(repository.existsByName(mockReqDTO.name())).thenReturn(false);
        when(repository.save(any(ProductEntity.class))).thenReturn(savedEntity);

        ProductDTOResponse resultDTO = service.store(mockReqDTO);

        verify(repository).existsByName(mockReqDTO.name());
        verify(repository).save(any(ProductEntity.class));
        assertThat(resultDTO.name(), is(equalTo(expectedDTO.name())));
    }

    @Test
    void testStore_shouldThrowConflict_whenNameAlreadyExists() {
        ProductDTORequest mockReqDTO = ProductTestData.samplePOSTRequestDTO();

        when(repository.existsByName(mockReqDTO.name())).thenReturn(true);

        ProductExceptionConflict exc = assertThrows(
            ProductExceptionConflict.class,
            () -> service.store(mockReqDTO)
        );

        assertThat(exc.getMessage(), is(equalTo("Product already exists.")));
        verify(repository, never()).save(any());
    }

    @Test
    void testUpdate_shouldUpdateTheProduct() {
        Long id = 1L;
        ProductEntity originalEntity = sampleEntities.get(0);
        ProductDTOPatchRequest mockPatchDTO = ProductTestData.sampleAvailableUpdateReq();
        ProductEntity savedEntity = sampleEntities.get(0);
        savedEntity.setAvailable(false);
        ProductDTOResponse expectedDTO = ProductMapper.toDTO(savedEntity);

        when(repository.findById(id)).thenReturn(Optional.of(originalEntity));
        when(repository.save(any(ProductEntity.class))).thenReturn(savedEntity);

        ProductDTOResponse resultDTO = service.update(id, mockPatchDTO);

        verify(repository).findById(id);
        verify(repository).save(any(ProductEntity.class));
        assertThat(resultDTO, is(equalTo(expectedDTO)));
    }

    @Test
    void testUpdate_shouldThrowNotFound_whenIdDoesNotExist() {
        Long missingId = 500L;
        ProductDTOPatchRequest mockPatchDTO = ProductTestData.sampleNameUpdateReq();

        when(repository.findById(missingId)).thenReturn(Optional.empty());

        ProductExceptionNotFound exc = assertThrows(
            ProductExceptionNotFound.class,
            () -> service.update(missingId, mockPatchDTO)
        );

        assertThat(exc.getMessage(), is(equalTo("Product " + missingId + " is not found")));
        verify(repository, never()).save(any());
    }


}
