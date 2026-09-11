package com.micarro.backend.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.micarro.backend.model.ShoppingMode;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "shopping_plan_history")
public class ShoppingPlanHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private BigDecimal budget;

    @Column(name = "estimated_total", nullable = false)
    private BigDecimal estimatedTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShoppingMode mode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(
            mappedBy = "history",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<ShoppingPlanHistoryItem> items = new ArrayList<>();

    public ShoppingPlanHistory() {
    }

    @PrePersist
    private void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void addItem(ShoppingPlanHistoryItem item) {
        items.add(item);
        item.setHistory(this);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public BigDecimal getEstimatedTotal() {
        return estimatedTotal;
    }

    public void setEstimatedTotal(BigDecimal estimatedTotal) {
        this.estimatedTotal = estimatedTotal;
    }

    public ShoppingMode getMode() {
        return mode;
    }

    public void setMode(ShoppingMode mode) {
        this.mode = mode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<ShoppingPlanHistoryItem> getItems() {
        return items;
    }

    public void setItems(List<ShoppingPlanHistoryItem> items) {
        this.items = items;
    }
}
