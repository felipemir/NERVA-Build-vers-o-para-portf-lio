package com.nervatech.nerva_build.service;

import com.nervatech.nerva_build.domain.model.Category;
import com.nervatech.nerva_build.dto.CategoryRequestDTO;
import com.nervatech.nerva_build.dto.CategoryResponseDTO;
import com.nervatech.nerva_build.repository.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public CategoryResponseDTO create(Long storeId, CategoryRequestDTO request) {
        String slug = resolveSlug(request.name(), request.slug());
        validateSlugUniqueness(storeId, slug, null);

        Category category = new Category();
        category.setStoreId(storeId);
        applyChanges(category, request, slug);

        return toResponse(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> listActiveByStore(Long storeId) {
        return categoryRepository.findByStoreIdAndActiveTrueOrderByNameAsc(storeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> listAllByStore(Long storeId) {
        return categoryRepository.findByStoreIdOrderByNameAsc(storeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponseDTO getById(Long storeId, Long categoryId) {
        return toResponse(findEntity(storeId, categoryId));
    }

    @Transactional(readOnly = true)
    public CategoryResponseDTO getBySlug(Long storeId, String slug) {
        return categoryRepository.findByStoreIdAndSlugAndActiveTrue(storeId, slug)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria nao encontrada"));
    }

    @Transactional
    public CategoryResponseDTO update(Long storeId, Long categoryId, CategoryRequestDTO request) {
        Category category = findEntity(storeId, categoryId);
        String slug = resolveSlug(request.name(), request.slug());
        validateSlugUniqueness(storeId, slug, categoryId);

        applyChanges(category, request, slug);
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deactivate(Long storeId, Long categoryId) {
        Category category = findEntity(storeId, categoryId);
        category.setActive(false);
        categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public void ensureCategoryBelongsToStore(Long storeId, Long categoryId) {
        if (categoryId != null) {
            findEntity(storeId, categoryId);
        }
    }

    private Category findEntity(Long storeId, Long categoryId) {
        return categoryRepository.findByIdAndStoreId(categoryId, storeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria nao encontrada"));
    }

    private void applyChanges(Category category, CategoryRequestDTO request, String slug) {
        category.setName(request.name().trim());
        category.setSlug(slug);
        category.setDescription(trimToNull(request.description()));
        category.setActive(request.active() == null ? Boolean.TRUE : request.active());
    }

    private void validateSlugUniqueness(Long storeId, String slug, Long currentCategoryId) {
        boolean exists = currentCategoryId == null
                ? categoryRepository.existsByStoreIdAndSlug(storeId, slug)
                : categoryRepository.existsByStoreIdAndSlugAndIdNot(storeId, slug, currentCategoryId);

        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ja existe categoria com este slug na loja");
        }
    }

    private String resolveSlug(String name, String slug) {
        String baseValue = trimToNull(slug);
        if (baseValue == null) {
            baseValue = trimToNull(name);
        }

        String normalized = Normalizer.normalize(baseValue, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nao foi possivel gerar slug para a categoria");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private CategoryResponseDTO toResponse(Category category) {
        return new CategoryResponseDTO(
                category.getId(),
                category.getStoreId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getActive()
        );
    }
}
