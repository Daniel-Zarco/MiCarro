package com.micarro.backend.dto;

public class CategoryCount {

    private final String category;
    private final long count;

    public CategoryCount(String category, long count) {
        this.category = category;
        this.count = count;
    }

    public String category() {
        return category;
    }

    public long count() {
        return count;
    }
}