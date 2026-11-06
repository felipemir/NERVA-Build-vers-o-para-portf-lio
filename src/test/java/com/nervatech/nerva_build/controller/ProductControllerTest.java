package com.nervatech.nerva_build.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nervatech.nerva_build.domain.model.AppUser;
import com.nervatech.nerva_build.domain.model.Category;
import com.nervatech.nerva_build.domain.model.Product;
import com.nervatech.nerva_build.domain.model.Store;
import com.nervatech.nerva_build.dto.ProductRequestDTO;
import com.nervatech.nerva_build.repository.AppUserRepository;
import com.nervatech.nerva_build.repository.CategoryRepository;
import com.nervatech.nerva_build.repository.ProductRepository;
import com.nervatech.nerva_build.repository.StoreRepository;
import com.nervatech.nerva_build.security.AppUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Product Controller Tests")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    private Store store1;
    private Store store2;
    private AppUserDetails adminStore1;
    private AppUserDetails adminStore2;
    private Category categoryStore1;
    private Category categoryStore2;

    @BeforeEach
    void setUp() {
        store1 = createStore("Store 1", "store1");
        store2 = createStore("Store 2", "store2");

        adminStore1 = createUser(store1, "admin1@test.com", "STORE_ADMIN");
        adminStore2 = createUser(store2, "admin2@test.com", "STORE_ADMIN");

        categoryStore1 = createCategory(store1, "Category Store 1", "cat-1");
        categoryStore2 = createCategory(store2, "Category Store 2", "cat-2");
    }

    // Helper methods to create entities
    private Store createStore(String name, String subdomain) {
        Store store = new Store();
        store.setName(name);
        store.setSubdomain(subdomain);
        return storeRepository.save(store);
    }

    private AppUserDetails createUser(Store store, String email, String role) {
        AppUser user = new AppUser();
        user.setStoreId(store.getId());
        user.setName("Test User");
        user.setEmail(email);
        user.setPassword("password");
        user.setRole(role);
        return new AppUserDetails(appUserRepository.save(user));
    }

    private Category createCategory(Store store, String name, String slug) {
        Category category = new Category();
        category.setStoreId(store.getId());
        category.setName(name);
        category.setSlug(slug);
        return categoryRepository.save(category);
    }

    private Product createProduct(Store store, Category category, String name, String sku, boolean active) {
        Product product = new Product();
        product.setStore(store);
        product.setCategoryId(category.getId());
        product.setName(name);
        product.setSku(sku);
        product.setPrice(BigDecimal.TEN);
        product.setStockQuantity(BigDecimal.valueOf(100));
        product.setActive(active);
        return productRepository.save(product);
    }

    @Nested
    @DisplayName("Admin: Create Product")
    class CreateProduct {

        @Test
        @DisplayName("Should create product successfully")
        void create_shouldCreateProduct() throws Exception {
            ProductRequestDTO request = new ProductRequestDTO("New Product", "A great product", "Votoran", "SACO", "SKU-001",
                    BigDecimal.valueOf(99.99), BigDecimal.valueOf(50), BigDecimal.valueOf(50), BigDecimal.valueOf(40),
                    BigDecimal.valueOf(30), BigDecimal.valueOf(12), BigDecimal.ONE, null, categoryStore1.getId(), true, true, true);

            mockMvc.perform(post("/api/products")
                            .with(user(adminStore1))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name", is("New Product")))
                    .andExpect(jsonPath("$.sku", is("SKU-001")))
                    .andExpect(jsonPath("$.categoryId", is(categoryStore1.getId().intValue())));
        }

        @Test
        @DisplayName("Should return Conflict when SKU already exists in the same store")
        void create_shouldReturnConflict_whenSkuExists() throws Exception {
            createProduct(store1, categoryStore1, "Existing Product", "SKU-EXISTING", true);
            ProductRequestDTO request = new ProductRequestDTO("Another Product", null, "Marca", "UN", "SKU-EXISTING", BigDecimal.TEN, BigDecimal.ONE, null, null, null, null, BigDecimal.ONE, null, categoryStore1.getId(), true, true, true);

            mockMvc.perform(post("/api/products")
                            .with(user(adminStore1))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should allow creating same SKU in a different store")
        void create_shouldAllowSameSkuInDifferentStores() throws Exception {
            createProduct(store1, categoryStore1, "Product in Store 1", "SKU-SHARED", true);
            ProductRequestDTO request = new ProductRequestDTO("Product in Store 2", null, "Marca", "UN", "SKU-SHARED", BigDecimal.TEN, BigDecimal.ONE, null, null, null, null, BigDecimal.ONE, null, categoryStore2.getId(), true, true, true);

            mockMvc.perform(post("/api/products")
                            .with(user(adminStore2)) // User from store 2
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should return Not Found when category belongs to another store")
        void create_shouldReturnNotFound_whenCategoryIsFromAnotherStore() throws Exception {
            ProductRequestDTO request = new ProductRequestDTO("Product with wrong category", null, "Marca", "UN", "SKU-WRONG-CAT", BigDecimal.TEN, BigDecimal.ONE, null, null, null, null, BigDecimal.ONE, null, categoryStore2.getId(), true, true, true);

            mockMvc.perform(post("/api/products")
                            .with(user(adminStore1)) // User from store 1
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return Bad Request with field errors for invalid data")
        void create_shouldReturnBadRequest_forInvalidData() throws Exception {
            ProductRequestDTO request = new ProductRequestDTO("", null, null, "", null, BigDecimal.valueOf(-10), null, null, null, null, null, null, null, null, true, true, true);

            mockMvc.perform(post("/api/products")
                            .with(user(adminStore1))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')].message", hasSize(1)))
                    .andExpect(jsonPath("$.fieldErrors[?(@.field == 'price')].message", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("Admin: Update Product")
    class UpdateProduct {
        private Product productToUpdate;

        @BeforeEach
        void setUp() {
            productToUpdate = createProduct(store1, categoryStore1, "Original Product", "SKU-UPDATE", true);
        }

        @Test
        @DisplayName("Should update product successfully")
        void update_shouldUpdateProduct() throws Exception {
            ProductRequestDTO request = new ProductRequestDTO("Updated Name", "Updated Desc", "Tigre", "BARRA", "SKU-UPDATED",
                    BigDecimal.valueOf(120.50), BigDecimal.valueOf(30), BigDecimal.valueOf(2), BigDecimal.valueOf(300),
                    BigDecimal.valueOf(10), BigDecimal.valueOf(10), BigDecimal.ONE, null, categoryStore1.getId(), true, false, false);

            mockMvc.perform(put("/api/products/{id}", productToUpdate.getId())
                            .with(user(adminStore1))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("Updated Name")))
                    .andExpect(jsonPath("$.sku", is("SKU-UPDATED")))
                    .andExpect(jsonPath("$.active", is(false)));
        }

        @Test
        @DisplayName("Should return Conflict when updating to an existing SKU")
        void update_shouldReturnConflict_forExistingSku() throws Exception {
            createProduct(store1, categoryStore1, "Another Product", "SKU-EXISTING", true);
            ProductRequestDTO request = new ProductRequestDTO("Updated Name", null, "Marca", "UN", "SKU-EXISTING", BigDecimal.TEN, BigDecimal.TEN, null, null, null, null, BigDecimal.ONE, null, categoryStore1.getId(), true, true, true);

            mockMvc.perform(put("/api/products/{id}", productToUpdate.getId())
                            .with(user(adminStore1))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should return Not Found when trying to update product from another store")
        void update_shouldReturnNotFound_forProductInAnotherStore() throws Exception {
            ProductRequestDTO request = new ProductRequestDTO("Attempted Update", null, "Marca", "UN", "SKU-FAIL", BigDecimal.ONE, BigDecimal.ONE, null, null, null, null, BigDecimal.ONE, null, categoryStore2.getId(), true, true, true);

            mockMvc.perform(put("/api/products/{id}", productToUpdate.getId())
                            .with(user(adminStore2)) // User from store 2
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Admin: Read Products")
    class ReadProducts {
        private Product p1;

        @BeforeEach
        void setUp() {
            p1 = createProduct(store1, categoryStore1, "Product A", "SKU-A", true);
            createProduct(store1, categoryStore1, "Product B", "SKU-B", true);
            createProduct(store1, categoryStore1, "Product C", "SKU-C", false);
            // Product from another store
            createProduct(store2, categoryStore2, "Other Store Product", "SKU-OTHER", true);
        }

        @Test
        @DisplayName("Should list all products for admin, including inactive")
        void getProductsForAdmin_shouldListAll() throws Exception {
            mockMvc.perform(get("/api/products/admin")
                            .with(user(adminStore1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)));
        }

        @Test
        @DisplayName("Should get product by ID for admin")
        void getProductForAdmin_shouldReturnCorrectProduct() throws Exception {
            mockMvc.perform(get("/api/products/admin/{id}", p1.getId())
                            .with(user(adminStore1)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(p1.getId().intValue())))
                    .andExpect(jsonPath("$.name", is("Product A")));
        }

        @Test
        @DisplayName("Should return Not Found when getting product from another store")
        void getProductForAdmin_shouldReturnNotFound_forOtherStore() throws Exception {
            mockMvc.perform(get("/api/products/admin/{id}", p1.getId())
                            .with(user(adminStore2))) // User from store 2
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Admin: Delete (Deactivate) Product")
    class DeactivateProduct {
        private Product product;

        @BeforeEach
        void setUp() {
            product = createProduct(store1, categoryStore1, "Deletable", "SKU-DEL", true);
        }

        @Test
        @DisplayName("Should deactivate product successfully")
        void delete_shouldDeactivateProduct() throws Exception {
            mockMvc.perform(delete("/api/products/{id}", product.getId())
                            .with(user(adminStore1)))
                    .andExpect(status().isNoContent());

            Product deactivated = productRepository.findById(product.getId()).orElseThrow();
            assertThat(deactivated.getActive()).isFalse();
        }

        @Test
        @DisplayName("Should return Not Found when deactivating product from another store")
        void delete_shouldReturnNotFound_forOtherStore() throws Exception {
            mockMvc.perform(delete("/api/products/{id}", product.getId())
                            .with(user(adminStore2))) // User from store 2
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Public: Showcase Endpoints")
    class PublicShowcase {
        private Product p1_active, p2_inactive;

        @BeforeEach
        void setUp() {
            p1_active = createProduct(store1, categoryStore1, "Public Product", "SKU-PUBLIC", true);
            p2_inactive = createProduct(store1, categoryStore1, "Inactive Product", "SKU-INACTIVE", false);
            createProduct(store2, categoryStore2, "Other Store Product", "SKU-OTHER", true);
        }

        @Test
        @DisplayName("Should list only active products from the specified store")
        void listPublic_shouldReturnOnlyActiveProducts() throws Exception {
            mockMvc.perform(get("/api/products")
                            .param("storeId", store1.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is(p1_active.getId().intValue())));
        }

        @Test
        @DisplayName("Should find active product by ID with public construction fields")
        void getPublicById_shouldReturnActiveProduct() throws Exception {
            mockMvc.perform(get("/api/products/{id}", p1_active.getId())
                            .param("storeId", store1.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("Public Product")))
                    .andExpect(jsonPath("$.sku").doesNotExist())
                    .andExpect(jsonPath("$.unit", is("UN")))
                    .andExpect(jsonPath("$.stockQuantity", is(100)))
                    .andExpect(jsonPath("$.minimumOrderQuantity", is(1)))
                    .andExpect(jsonPath("$.deliveryAvailable", is(true)))
                    .andExpect(jsonPath("$.pickupAvailable", is(true)));
        }

        @Test
        @DisplayName("Should return Not Found for inactive product by ID")
        void getPublicById_shouldReturnNotFound_forInactiveProduct() throws Exception {
            mockMvc.perform(get("/api/products/{id}", p2_inactive.getId())
                            .param("storeId", store1.getId().toString()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return Not Found for product from another store")
        void getPublicById_shouldReturnNotFound_forOtherStoreProduct() throws Exception {
            mockMvc.perform(get("/api/products/{id}", p1_active.getId())
                            .param("storeId", store2.getId().toString())) // Requesting for store 2
                    .andExpect(status().isNotFound());
        }
    }
}
