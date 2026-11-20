package com.cn01.solution.entity;

public enum BillingType {
    ONE_TIME("一次性买断", "套"),
    YEARLY("按年订阅", "年");

    private final String label;
    private final String unit;

    BillingType(String label, String unit) {
        this.label = label;
        this.unit = unit;
    }

    public String label() { return label; }
    /** 版本数量的单位：买断按"套"，订阅按"年" */
    public String unit() { return unit; }
}
