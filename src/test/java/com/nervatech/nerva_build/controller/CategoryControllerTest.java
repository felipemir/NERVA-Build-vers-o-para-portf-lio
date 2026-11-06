package com.nervatech.nerva_build.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nervatech.nerva_build.domain.model.AppUser;
import com.nervatech.nerva_build.domain.model.Category;
import com.nervatech.nerva_build.domain.model.Store;
import com.nervatech.nerva_build.dto.CategoryRequestDTO;
import com.nervatech.nerva_build.repository.AppUserRepository;
import com.nervatech.nerva_build.repository.CategoryRepository;
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
@DisplayName("Category Controller Tests")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    private Store store1;
    private Store store2;
    private AppUserDetails userStore1;
    private AppUserDetails userStore2;
    private AppUserDetails regularUser;


    @BeforeEach
    void setUp() {
        // Create Stores
        store1 = new Store();
        store1.setName("Store 1");
        store1.setSubdomain("store1");
        store1 = storeRepository.save(store1);

        store2 = new Store();
        store2.setName("Store 2");
        store2.setSubdomain("store2");
        store2 = storeRepository.save(store2);

        // Create Users for each store
        AppUser appUser1 = new AppUser();
        appUser1.setStoreId(store1.getId());
        appUser1.setName("Admin 1");
        appUser1.setEmail("admin1@test.com");
        appUser1.setPassword("password");
        appUser1.setRole("STORE_ADMIN");
        appUser1 = appUserRepository.save(appUser1);
        userStore1 = new AppUserDetails(appUser1);

        AppUser appUser2 = new AppUser();
        appUser2.setStoreId(store2.getId());
        appUser2.setName("Admin 2");
        appUser2.setEmail("admin2@test.com");
        appUser2.setPassword("password");
        appUser2.setRole("STORE_ADMIN");
        appUser2 = appUserRepository.save(appUser2);
        userStore2 = new AppUserDetails(appUser2);

        AppUser appUser3 = new AppUser();
        appUser3.setStoreId(store1.getId());
        appUser3.setName("Regular User");
        appUser3.setEmail("user@test.com");
        appUser3.setPassword("password");
        appUser3.setRole("SELLER"); // Correct Role
        appUser3 = appUserRepository.save(appUser3);
        regularUser = new AppUserDetails(appUser3);
    }

    private Category createCategory(Store store, String name, String slug, boolean active) {
        Category category = new Category();
        category.setStoreId(store.getId());
        category.setName(name);
        category.setSlug(slug);
        category.setActive(active);
        return categoryRepository.save(category);
    }

    @Nested
    @DisplayName("Create Category")
    class CreateCategory {

        @Test
        @DisplayName("Should create category when user is admin")
        void create_shouldReturnCreated_whenUserIsAdmin() throws Exception {
            CategoryRequestDTO request = new CategoryRequestDTO("Eletrônicos", "eletronicos", "desc", true);

            mockMvc.perform(post("/api/categories")
                            .with(user(userStore1))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name", is("Eletrônicos")))
                    .andExpect(jsonPath("$.slug", is("eletronicos")))
                    .andExpect(jsonPath("$.storeId", is(store1.getId().intValue())));
        }

        @Test
        @DisplayName("Should return Forbidden when user is not admin")
        void create_shouldReturnForbidden_whenUserIsNotAdmin() throws Exception {
            CategoryRequestDTO request = new CategoryRequestDTO("Eletrônicos", null, null, true);

            mockMvc.perform(post("/api/categories")
                            .with(user(regularUser)) // Use a real user with SELLER role
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return Forbidden when user is not authenticated")
        void create_shouldReturnForbidden_whenNotAuthenticated() throws Exception {
            CategoryRequestDTO request = new CategoryRequestDTO("Eletrônicos", null, null, true);

            // No .with(user(...)) means the request is anonymous
            mockMvc.perform(post("/api/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden()); // Spring Security treats anonymous as authenticated but without roles, thus 403
        }

        @Test
        @DisplayName("Should return Conflict when slug already exists in the same store")
        void create_shouldReturnConflict_whenSlugExistsInStore() throws Exception {
            createCategory(store1, "Móveis", "moveis", true);
            CategoryRequestDTO request = new CategoryRequestDTO("Outra Categoria", "moveis", null, true);

            mockMvc.perform(post("/api/categories")
                            .with(user(userStore1))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Update Category")
    class UpdateCategory {
        private Category category;

        @BeforeEach
        void setUp() {
            category = createCategory(store1, "Original", "original", true);
        }

        @Test
        @DisplayName("Should update category successfully")
        void update_shouldUpdateCategory() throws Exception {
            CategoryRequestDTO request = new CategoryRequestDTO("Nome Atualizado", "slug-atualizado", "desc", false);

            mockMvc.perform(put("/api/categories/{id}", category.getId())
                            .with(user(userStore1))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("Nome Atualizado")))
                    .andExpect(jsonPath("$.slug", is("slug-atualizado")))
                    .andExpect(jsonPath("$.active", is(false)));
        }

        @Test
        @DisplayName("Should return Not Found when trying to update category from another store")
        void update_shouldReturnNotFound_forCategoryInAnotherStore() throws Exception {
            CategoryRequestDTO request = new CategoryRequestDTO("Update", "update", null, true);

            mockMvc.perform(put("/api/categories/{id}", category.getId())
                            .with(user(userStore2)) // User from another store
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Read Categories")
    class ReadCategories {
        private Category cat1, cat2, cat3_inactive;

        @BeforeEach
        void setUp() {
            cat1 = createCategory(store1, "Active 1", "active-1", true);
            cat2 = createCategory(store1, "Active 2", "active-2", true);
            cat3_inactive = createCategory(store1, "Inactive", "inactive", false);
            // Category from another store
            createCategory(store2, "Other Store Cat", "other-store-cat", true);
        }

        @Test
        @DisplayName("Should return only active categories for public listing")
        void listActive_shouldReturnOnlyActiveCategories() throws Exception {
            mockMvc.perform(get("/api/categories")
                            .param("storeId", store1.getId().toString())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].name", is("Active 1")))
                    .andExpect(jsonPath("$[1].name", is("Active 2")));
        }

        @Test
        @DisplayName("Should return all categories for admin listing")
        void listAllForAdmin_shouldReturnAllCategoriesFromStore() throws Exception {
            mockMvc.perform(get("/api/categories/admin")
                            .with(user(userStore1))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)));
        }

        @Test
        @DisplayName("Should get category by ID for admin")
        void getForAdmin_shouldReturnCategory() throws Exception {
            mockMvc.perform(get("/api/categories/admin/{id}", cat1.getId())
                            .with(user(userStore1))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(cat1.getId().intValue())));
        }

        @Test
        @DisplayName("Should return Not Found when getting category from another store")
        void getForAdmin_shouldReturnNotFound_forCategoryInAnotherStore() throws Exception {
            mockMvc.perform(get("/api/categories/admin/{id}", cat1.getId())
                            .with(user(userStore2)) // User from another store
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Delete (Deactivate) Category")
    class DeactivateCategory {
        private Category category;

        @BeforeEach
        void setUp() {
            category = createCategory(store1, "To Be Deleted", "to-be-deleted", true);
        }

        @Test
        @DisplayName("Should deactivate category and return No Content")
        void delete_shouldDeactivateCategory() throws Exception {
            mockMvc.perform(delete("/api/categories/{id}", category.getId())
                            .with(user(userStore1)))
                    .andExpect(status().isNoContent());

            Category deactivatedCategory = categoryRepository.findById(category.getId()).orElseThrow();
            assertThat(deactivatedCategory.getActive()).isFalse();
        }

        @Test
        @DisplayName("Should return Not Found when trying to deactivate category from another store")
        void delete_shouldReturnNotFound_forCategoryInAnotherStore() throws Exception {
            mockMvc.perform(delete("/api/categories/{id}", category.getId())
                            .with(user(userStore2))) // User from another store
                    .andExpect(status().isNotFound());
        }
    }
}
