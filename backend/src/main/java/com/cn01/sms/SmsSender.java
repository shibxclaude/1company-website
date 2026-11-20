package com.cn01.sms;

/** 短信通道。上线时实现一个对接云短信服务的 Bean（加 @Primary）即可替换默认的日志实现。 */
public interface SmsSender {
    void sendCode(String phone, String code);

    void sendText(String phone, String text);
}
