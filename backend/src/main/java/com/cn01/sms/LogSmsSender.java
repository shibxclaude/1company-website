package com.cn01.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 默认实现：只写日志。手机号脱敏，验证码只在开发模式下才会出现在接口返回里。 */
@Slf4j
@Component
public class LogSmsSender implements SmsSender {
    @Override
    public void sendCode(String phone, String code) {
        log.info("[短信-模拟] 向 {} 发送验证码 {}", mask(phone), code);
    }

    @Override
    public void sendText(String phone, String text) {
        log.info("[短信-模拟] 向 {} 发送：{}", mask(phone), text);
    }

    public static String mask(String phone) {
        return phone == null || phone.length() < 7 ? phone : phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
