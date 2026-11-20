package com.cn01.site;

import com.cn01.common.Result;
import com.cn01.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 前台运行时配置：是否开放在线购买与会员服务（前台据此显示购买/登录入口或电话咨询）。 */
@RestController
@RequiredArgsConstructor
public class SiteConfigController {
    private final AppProperties props;

    public record SiteConfig(boolean shopEnabled, String servicePhone) {}

    @GetMapping("/api/portal/site-config")
    public Result<SiteConfig> config() {
        return Result.ok(new SiteConfig(props.getShop().isEnabled(), props.getShop().getServicePhone()));
    }
}
