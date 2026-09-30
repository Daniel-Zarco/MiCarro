package com.micarro.backend.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.micarro.backend.dto.ProductResponse;
import com.micarro.backend.dto.SavedPlanItemRequest;
import com.micarro.backend.dto.SavedPlanItemResponse;
import com.micarro.backend.dto.SavedPlanNameRequest;
import com.micarro.backend.dto.SavedPlanRequest;
import com.micarro.backend.dto.SavedPlanResponse;
import com.micarro.backend.dto.SavedPlanSummaryResponse;
import com.micarro.backend.entity.Product;
import com.micarro.backend.entity.SavedPlan;
import com.micarro.backend.entity.SavedPlanItem;
import com.micarro.backend.entity.User;
import com.micarro.backend.repository.ProductRepository;
import com.micarro.backend.repository.SavedPlanRepository;
import com.micarro.backend.repository.UserRepository;

/*
 * Planes guardados por el usuario. El propietario se resuelve siempre desde el
 * email del JWT (nunca de un userId enviado por el cliente) y todas las
 * operaciones comprueban la propiedad mediante findByIdAndUserId.
 */
@Service
public class SavedPlanService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final SavedPlanRepository savedPlanRepository;

    public SavedPlanService(
            UserRepository userRepository,
            ProductRepository productRepository,
            SavedPlanRepository savedPlanRepository) {

        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.savedPlanRepository = savedPlanRepository;
    }

    @Transactional
    public SavedPlanResponse create(String email, SavedPlanRequest request) {

        User user = requireUser(email);

        SavedPlan plan = new SavedPlan();
        plan.setUser(user);
        plan.setName(request.getName().trim());

        if (request.getItems() != null) {

            for (SavedPlanItemRequest itemRequest : request.getItems()) {
                plan.addItem(toItem(itemRequest));
            }
        }

        SavedPlan saved = savedPlanRepository.save(plan);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SavedPlanSummaryResponse> getPlans(String email) {

        User user = requireUser(email);

        return savedPlanRepository
                .findByUserIdOrderByCreatedAtDescIdDesc(user.getId())
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public SavedPlanResponse getPlanById(String email, Long id) {

        User user = requireUser(email);

        SavedPlan plan = findOwnedPlan(user.getId(), id);

        return toResponse(plan);
    }

    @Transactional
    public void delete(String email, Long id) {

        User user = requireUser(email);

        SavedPlan plan = findOwnedPlan(user.getId(), id);

        savedPlanRepository.delete(plan);
    }

    @Transactional
    public SavedPlanResponse rename(String email, Long id, SavedPlanNameRequest request) {

        User user = requireUser(email);

        SavedPlan plan = findOwnedPlan(user.getId(), id);

        plan.setName(request.getName().trim());

        SavedPlan saved = savedPlanRepository.save(plan);

        return toResponse(saved);
    }

    /*
     * Construye el snapshot del item consultando el catálogo actual: el
     * nombre, marca, formato, imagen y precio se toman del Product, nunca de
     * la petición. Un producto inexistente o inactivo no se puede guardar.
     */
    private SavedPlanItem toItem(SavedPlanItemRequest request) {

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Producto no disponible: " + request.getProductId()
                ));

        BigDecimal unitPrice = product.getPrice();
        BigDecimal subtotal = unitPrice == null
                ? null
                : unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

        SavedPlanItem item = new SavedPlanItem();

        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setBrand(product.getBrand());
        item.setFormat(product.getFormat());
        item.setImageUrl(product.getImageUrl());
        item.setQuantity(request.getQuantity());
        item.setUnitPrice(unitPrice);
        item.setSubtotal(subtotal);

        return item;
    }

    private SavedPlan findOwnedPlan(Long userId, Long id) {

        return savedPlanRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Plan no encontrado"
                ));
    }

    private User requireUser(String email) {

        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Usuario no autenticado"
                ));
    }

    private SavedPlanResponse toResponse(SavedPlan plan) {

        List<SavedPlanItemResponse> items = plan.getItems()
                .stream()
                .map(this::toItemResponse)
                .toList();

        return new SavedPlanResponse(
                plan.getId(),
                plan.getName(),
                plan.getCreatedAt(),
                items
        );
    }

    /*
     * El precio histórico del snapshot se conserva en unitPrice; el precio
     * actual se resuelve del catálogo (solo productos activos) para permitir
     * "añadir al carrito" con el precio vigente.
     */
    private SavedPlanItemResponse toItemResponse(SavedPlanItem item) {

        ProductResponse currentProduct = productRepository
                .findById(item.getProductId())
                .map(this::toProductResponse)
                .orElse(null);

        return new SavedPlanItemResponse(
                item.getProductId(),
                item.getProductName(),
                item.getBrand(),
                item.getFormat(),
                item.getImageUrl(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal(),
                currentProduct
        );
    }

    private ProductResponse toProductResponse(Product product) {

        return new ProductResponse(
                product.getId(),
                product.getExternalId(),
                product.getName(),
                product.getBrand(),
                product.getCategory(),
                product.getImageUrl(),
                product.getFormat(),
                product.getPrice()
        );
    }

    private SavedPlanSummaryResponse toSummary(SavedPlan plan) {

        int itemCount = plan.getItems().size();

        BigDecimal total = plan.getItems().stream()
                .map(SavedPlanItem::getSubtotal)
                .filter(subtotal -> subtotal != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SavedPlanSummaryResponse(
                plan.getId(),
                plan.getName(),
                plan.getCreatedAt(),
                itemCount,
                total
        );
    }
}