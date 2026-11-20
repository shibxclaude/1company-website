package com.cn01.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String siteUrl;
    private Jwt jwt = new Jwt();
    private Admin admin = new Admin();
    private Upload upload = new Upload();
    private Order order = new Order();
    private Sms sms = new Sms();
    private Pay pay = new Pay();
    private Notify notify = new Notify();

    @Data
    public static class Jwt {
        private String secret;
        private int adminTtlHours = 12;
        private int userTtlHours = 168;
    }

    @Data
    public static class Admin {
        private String initPassword;
    }

    @Data
    public static class Upload {
        private String dir;
        private String urlPrefix = "/uploads";
    }

    @Data
    public static class Order {
        private int expireMinutes = 30;
        private String agreementVersion = "v1.0";
        private int createLimitPerHour = 20;
    }

    @Data
    public static class Sms {
        private boolean mock = true;
    }

    @Data
    public static class Pay {
        private String mode = "mock";
        private String notifyUrl;
        private Wechat wechat = new Wechat();

        public boolean isMock() {
            return "mock".equalsIgnoreCase(mode);
        }
    }

    @Data
    public static class Wechat {
        private String appId;
        private String mchId;
        private String merchantSerialNumber;
        private String privateKeyPath;
        private String apiV3Key;
    }

    @Data
    public static class Notify {
        private String mailFrom;
        private String adminEmail;
    }
}
