package com.cn01.config;

import com.cn01.auth.AuthInterceptor;
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
    private final AppProperties props;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/api/**");
    }

    /** 生产环境 /uploads 由 nginx 直接托管；这里让本地开发也能访问上传的图片。 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String dir = Path.of(props.getUpload().getDir()).toAbsolutePath().toUri().toString();
        registry.addResourceHandler(props.getUpload().getUrlPrefix() + "/**").addResourceLocations(dir);
    }
}
