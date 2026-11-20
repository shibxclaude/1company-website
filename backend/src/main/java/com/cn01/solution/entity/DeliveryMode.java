package com.cn01.solution.entity;

public enum DeliveryMode {
    ONLINE_OPEN("在线开通"),
    DOWNLOAD_LICENSE("下载授权"),
    REMOTE_DEPLOY("远程部署");

    private final String label;

    DeliveryMode(String label) { this.label = label; }

    public String label() { return label; }
}
