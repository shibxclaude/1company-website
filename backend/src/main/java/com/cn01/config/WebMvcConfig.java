package com.cn01.config;

import com.cn01.auth.AuthInterceptor;
import com.cn01.site.ShopGate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;
    private final ShopGate shopGate;
    private final AppProperties props;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 先判断购买是否开放，再鉴权：关闭时统一返回"暂未开放"，而不是 401
        registry.addInterceptor(shopGate).addPathPatterns(ShopGate.PATHS);
        registry.addInterceptor(authInterceptor).addPathPatterns("/api/**");
    }

    /** 生产环境 /uploads 由 nginx 直接托管；这里让本地开发也能访问上传的图片。 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String dir = Path.of(props.getUpload().getDir()).toAbsolutePath().toUri().toString();
        registry.addResourceHandler(props.getUpload().getUrlPrefix() + "/**").addResourceLocations(dir);
    }
}
