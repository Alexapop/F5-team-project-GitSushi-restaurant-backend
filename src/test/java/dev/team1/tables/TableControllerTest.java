package dev.team1.tables;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.team1.security.JwtFilter;
import dev.team1.security.SecurityConfiguration;
import dev.team1.tables.dtos.TableDTOResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import org.springframework.security.test.context.support.WithMockUser;

@WebMvcTest(controllers = TableController.class, properties = "api-endpoint=api/v1")
@Import(SecurityConfiguration.class)
class TableControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    JwtFilter jwtFilter; 

    @MockitoBean
    private TableService tableService;

    @BeforeEach 
    void setup() throws Exception {
        doAnswer(invocation -> {
            ServletRequest req = invocation.getArgument(0);
            ServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    @Test
    @WithMockUser("CUSTOMER")
    void getCurrentTableReturnsAssociatedTable() throws Exception {
        when(tableService.getTableByDeviceIdentifier("tablet-12"))
                .thenReturn(new TableDTOResponse(12));

        mockMvc.perform(get("/api/v1/tables/by-device")
                        .header("Device-Identifier", "tablet-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tableNumber").value(12));

        verify(tableService).getTableByDeviceIdentifier("tablet-12");
    }

    @Test
    @WithMockUser("CUSTOMER")
    void getCurrentTableRejectsMissingDeviceIdentifier() throws Exception {
        mockMvc.perform(get("/api/v1/tables/by-device"))
                .andExpect(status().isBadRequest());
    }
}
