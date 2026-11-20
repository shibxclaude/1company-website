package com.cn01.auth;

public record AuthPrincipal(long id, Type type, String name) {
    public enum Type { ADMIN, USER }
}
