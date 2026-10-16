package com.ecom.orders.controllers;

import com.ecom.orders.auth.AuthService;
import com.ecom.orders.auth.AuthUserDto;
import com.ecom.orders.auth.UnauthorizedException;
import com.ecom.orders.models.Order;
import com.ecom.orders.models.OrderStatus;
import com.ecom.orders.services.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP-level checks: authentication, the ADMIN role, the internal token and error mapping. */
@WebMvcTest(properties = "internal.api-token=test-secret")
class OrderControllersWebTest {
    @Autowired
    MockMvc mvc;
    @MockitoBean
    OrderService orderService;
    @MockitoBean
    AuthService authService;

    @BeforeEach
    void setUp() {
        when(authService.validateToken("customer")).thenReturn(user(7L, "CUSTOMER"));
        when(authService.validateToken("admin")).thenReturn(user(1L, "CUSTOMER", "ADMIN"));
        when(authService.validateToken("bad")).thenThrow(new UnauthorizedException("Invalid or expired token"));
    }

    private static AuthUserDto user(Long id, String... roles) {
        AuthUserDto u = new AuthUserDto();
        u.setId(id);
        u.setRoles(List.of(roles));
        return u;
    }

    private static Order order(OrderStatus status) {
        Order o = new Order();
        o.setId(1L);
        o.setStatus(status);
        return o;
    }

    @Test
    void customerEndpointsNeedAValidToken() throws Exception {
        mvc.perform(get("/api/v1/orders")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/orders").header("Authorization", "Bearer bad")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/orders").header("Authorization", "Bearer customer")).andExpect(status().isOk());
    }

    @Test
    void adminEndpointsRequireTheAdminRole() throws Exception {
        mvc.perform(get("/api/v1/admin/orders").header("Authorization", "Bearer customer"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorMessage").value("Administrator role required"));
        mvc.perform(put("/api/v1/admin/orders/1/status").header("Authorization", "Bearer customer")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isForbidden());
        verify(orderService, never()).updateStatus(anyLong(), any(), any());

        when(orderService.getAllOrders(OrderStatus.PAID)).thenReturn(List.of(order(OrderStatus.PAID)));
        mvc.perform(get("/api/v1/admin/orders?status=PAID").header("Authorization", "Bearer admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PAID"));
    }

    @Test
    void invalidParametersGiveACleanMessage() throws Exception {
        mvc.perform(get("/api/v1/admin/orders?status=NOPE").header("Authorization", "Bearer admin"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Invalid value 'NOPE' for status"));
        mvc.perform(get("/api/v1/orders/abc").header("Authorization", "Bearer customer"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorMessage").value("Invalid value 'abc' for id"));
    }

    @Test
    void internalEndpointsRequireTheSharedSecret() throws Exception {
        String body = "{\"paymentId\":\"p1\",\"amount\":1000}";
        mvc.perform(post("/internal/api/v1/orders/1/payment-completed")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/internal/api/v1/orders/1/payment-completed").header("X-Internal-Token", "wrong")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        verify(orderService, never()).markPaid(anyLong(), any(), any());

        when(orderService.markPaid(eq(1L), eq("p1"), eq(1000L))).thenReturn(order(OrderStatus.PAID));
        mvc.perform(post("/internal/api/v1/orders/1/payment-completed").header("X-Internal-Token", "test-secret")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void userTokensDoNotOpenInternalEndpoints() throws Exception {
        mvc.perform(put("/internal/api/v1/orders/1/status").header("Authorization", "Bearer admin")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isUnauthorized());
    }
}
