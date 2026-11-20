package com.cn01.auth;

/** ver：签发时账号的 token_version，改密码后版本递增，旧 token 随之失效 */
public record AuthPrincipal(long id, Type type, String name, int ver) {
    public enum Type { ADMIN, USER }

    public static int ver(Integer tokenVersion) {
        return tokenVersion == null ? 0 : tokenVersion;
    }
}
