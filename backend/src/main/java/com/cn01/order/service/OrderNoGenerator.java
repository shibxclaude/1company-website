package com.cn01.order.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** 订单号 = 日期时间(yyyyMMddHHmmss, 14 位) + 业务码(2 位) + 随机数(6 位)，共 22 位。 */
public final class OrderNoGenerator {
    /** 业务码 01：解决方案在线购买 */
    public static final String BIZ_SOLUTION = "01";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final SecureRandom RANDOM = new SecureRandom();

    private OrderNoGenerator() {}

    public static String next(String bizCode, LocalDateTime now) {
        return now.format(FMT) + bizCode + String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
