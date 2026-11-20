package com.cn01;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan(basePackages = "com.cn01", markerInterface = BaseMapper.class)
@EnableCaching
@EnableScheduling
@EnableAsync
public class Cn01Application {
    /** 业务时间统一为北京时间：JVM、JDBC 会话（见 datasource url）、Jackson 三处一致 */
    public static final String ZONE = "Asia/Shanghai";

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(ZONE));
        SpringApplication.run(Cn01Application.class, args);
    }
}
