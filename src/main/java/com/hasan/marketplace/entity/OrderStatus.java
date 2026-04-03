package com.hasan.marketplace.entity;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED;

    public boolean canSellerTransitionTo(OrderStatus targetStatus) {
        return switch (this) {
            case PENDING -> targetStatus == CONFIRMED;
            case CONFIRMED -> targetStatus == OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> targetStatus == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }

    public String getDisplayName() {
        return name().replace('_', ' ');
    }
}
