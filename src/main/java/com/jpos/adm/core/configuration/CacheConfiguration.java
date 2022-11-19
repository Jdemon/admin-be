package com.jpos.adm.core.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.StringJoiner;

@EnableCaching
@Configuration
@Slf4j
public class CacheConfiguration extends CachingConfigurerSupport {

    @Value("${spring.admin-be.cache.timeout:5m}")
    private Duration timeoutTTL;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("admin");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterAccess(timeoutTTL)
        );
        return cacheManager;
    }

    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            StringJoiner joiner = new StringJoiner("_");
            joiner.add(target.getClass().getSimpleName());
            if (List.of("patch","update","delete").contains(method.getName()) && params != null && params.length > 0) {
                joiner.add(params[0].toString());
            } else {
                joiner.add(StringUtils.arrayToDelimitedString(params, "_"));
            }
            String key = joiner.toString();
            log.info("key :" + key);
            return key;
        };
    }
}
