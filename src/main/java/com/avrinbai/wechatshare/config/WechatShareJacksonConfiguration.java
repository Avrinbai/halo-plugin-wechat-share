package com.avrinbai.wechatshare.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WechatShareJacksonConfiguration {

    /**
     * 供插件内组件（如分享页 JSON 序列化）注入；与 Halo 主应用的 ObjectMapper 相互独立。
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
