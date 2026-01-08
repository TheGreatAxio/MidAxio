package com.axios.midaxio.model;

public enum Role {
    USER,
    ADMIN,
    MODERATOR;

    public String getRoleName() {
        return "ROLE_" + this.name();
    }
}