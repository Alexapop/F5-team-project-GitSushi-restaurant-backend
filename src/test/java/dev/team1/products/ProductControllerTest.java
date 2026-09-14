package dev.team1.products;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.equalTo;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.Collectors;

import dev.team1.config.SecurityConfiguration;
import dev.team1.contracts.IProductService;
import dev.team1.enums.ProductCategory;
import dev.team1.products.dtos.ProductDTOResponse;
import dev.team1.products.exceptions.ProductExceptionNotFound;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;

@WebMvcTest(controllers = ProductController.class)
@Import(SecurityConfiguration.class)
public class ProductControllerTest {

    @Autowired 
    MockMvc mockMvc;

    @MockitoBean 
    IProductService service;

    @Autowired 
    ObjectMapper mapper;

    private List<ProductDTOResponse> mockProducts;

    @BeforeEach 
    void setup() {
        mockProducts = ProductTestData.sampleDTOs();
    }

    @Test 
    void testIndex_shouldReturnAllAvailableFirstPage() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        
        List<ProductDTOResponse> mockContent = mockProducts.stream()
            .filter(p -> p.available())
            .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), mockContent.size());

        List<ProductDTOResponse> mockPageContent = mockContent.subList(start, end);
        
        Page<ProductDTOResponse> mockPage = new PageImpl<>(mockPageContent, pageable, mockContent.size());
        String json = mapper.writeValueAsString(mockPage);

        when(service.getAllAvailable(pageable)).thenReturn(mockPage);
        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/products"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

        JsonNode tree = mapper.readTree(response.getContentAsString());

        List<ProductDTOResponse> respContent = mapper.convertValue(
            tree.get("content"),
            new TypeReference<List<ProductDTOResponse>>() {}
        );

        assertThat(response.getContentAsString(), is(equalTo(json)));
        assertThat(respContent, is(equalTo(mockPageContent)));
        assertThat(response.getStatus(), is(equalTo(HttpStatus.OK.value())));
        assertThat(tree.get("totalPages").asInt(), is(equalTo(1)));
        assertThat(respContent.size(), is(equalTo(7)));
        assertThat(respContent.get(0).name(), is(equalTo("Ramen Fix")));
    }


    @Test 
    void testIndex_shouldReturnAllAvailableSecondPage() throws Exception {
        Pageable pageable = PageRequest.of(1, 4);
        
        List<ProductDTOResponse> mockContent = mockProducts.stream()
            .filter(p -> p.available())
            .collect(Collectors.toList());
        
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), mockContent.size());

        List<ProductDTOResponse> mockPageContent = mockContent.subList(start, end);

        Page<ProductDTOResponse> mockPage = new PageImpl<>(mockPageContent, pageable, mockContent.size());
        String json = mapper.writeValueAsString(mockPage);

        when(service.getAllAvailable(pageable)).thenReturn(mockPage);
        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/products")
                .param("page", "1")
                .param("size", "4")
                .accept(MediaType.ALL_VALUE))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

        JsonNode tree = mapper.readTree(response.getContentAsString());

        List<ProductDTOResponse> respContent = mapper.convertValue(
            tree.get("content"),
            new TypeReference<List<ProductDTOResponse>>() {}
        );

        assertThat(response.getContentAsString(), is(equalTo(json)));
        assertThat(respContent, is(equalTo(mockPageContent)));
        assertThat(response.getStatus(), is(equalTo(HttpStatus.OK.value())));
        assertThat(tree.get("totalPages").asInt(), is(equalTo(2)));
        assertThat(respContent.size(), is(equalTo(3)));
        assertThat(respContent.get(0).name(), is(equalTo("EbiOps")));
    }

    @Test 
    void testAdministration_shouldReturnAllProducts() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), mockProducts.size());

        List<ProductDTOResponse> mockPageContent = mockProducts.subList(start, end);
        
        Page<ProductDTOResponse> mockPage = new PageImpl<>(mockPageContent, pageable, mockProducts.size());
        String json = mapper.writeValueAsString(mockPage);

        when(service.getAll(pageable)).thenReturn(mockPage);
        // TODO: For ROLE_ADMIN only check
        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/products/administration"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

        JsonNode tree = mapper.readTree(response.getContentAsString());

        List<ProductDTOResponse> respContent = mapper.convertValue(
            tree.get("content"),
            new TypeReference<List<ProductDTOResponse>>() {}
        );

        assertThat(response.getContentAsString(), is(equalTo(json)));
        assertThat(respContent, is(equalTo(mockProducts)));
        assertThat(response.getStatus(), is(equalTo(HttpStatus.OK.value())));
        assertThat(tree.get("totalPages").asInt(), is(equalTo(1)));
        assertThat(respContent.size(), is(equalTo(8)));
        assertThat(respContent.get(1).name(), is(equalTo("Baked Python")));
        assertThat(respContent.get(1).available(), is(equalTo(false)));
    }

    @Test 
    void testGetById_shouldReturnProductById() throws Exception {

        ProductDTOResponse mockItem = mockProducts.get(3);
        // Java Roll
        
        String json = mapper.writeValueAsString(mockItem);

        when(service.getById(4L)).thenReturn(mockItem);
        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/products/4"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

        ProductDTOResponse respDTO = mapper.readValue(
            response.getContentAsString(),
            new TypeReference<ProductDTOResponse>() {}
        );

        assertThat(response.getContentAsString(), is(equalTo(json)));
        assertThat(respDTO, is(equalTo(mockItem)));
        assertThat(response.getStatus(), is(equalTo(HttpStatus.OK.value())));
        assertThat(respDTO.id(), is(equalTo(4L)));
        assertThat(respDTO.name(), is(equalTo("Java Roll")));

    }

    @Test
    void testGetById_shouldReturn404NotFound() throws Exception {
        Long missingId = 500L;
        when(service.getById(missingId))
            .thenThrow(new ProductExceptionNotFound(
                "Cannot find product with id 500 because it doesn't exist."
            ));

        mockMvc.perform(get("/api/v1/products/{id}", missingId))
            .andExpect(status().isNotFound());
    }


    @Test 
    void testIndex_shouldReturnCorrectCategory() throws Exception {

        Pageable pageable = PageRequest.of(0, 20);
        
        List<ProductDTOResponse> mockContent = mockProducts.stream()
            .filter(p -> p.available() && p.category() == ProductCategory.POSTRES)
            .collect(Collectors.toList());
        
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), mockContent.size());

        List<ProductDTOResponse> mockPageContent = mockContent.subList(start, end);

        Page<ProductDTOResponse> mockPage = new PageImpl<>(mockPageContent, pageable, mockContent.size());
        String json = mapper.writeValueAsString(mockPage);

        when(service.getByCategory(ProductCategory.POSTRES, pageable)).thenReturn(mockPage);
        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/products")
                .param("category", "POSTRES")
                .accept(MediaType.ALL_VALUE))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

        JsonNode tree = mapper.readTree(response.getContentAsString());

        List<ProductDTOResponse> respContent = mapper.convertValue(
            tree.get("content"),
            new TypeReference<List<ProductDTOResponse>>() {}
        );

        assertThat(response.getContentAsString(), is(equalTo(json)));
        assertThat(respContent, is(equalTo(mockPageContent)));
        assertThat(response.getStatus(), is(equalTo(HttpStatus.OK.value())));
        assertThat(tree.get("totalPages").asInt(), is(equalTo(1)));
        assertThat(respContent.size(), is(equalTo(2)));
        assertThat(respContent.get(0).name(), is(equalTo("Mochi Python")));

    }

}
