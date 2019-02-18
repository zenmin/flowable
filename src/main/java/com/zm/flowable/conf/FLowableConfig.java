package com.zm.flowable.conf;

import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Configuration;

/**
 * @Describle This Class Is
 * @Author ZengMin
 * @Date 2019/2/15 11:40
 */
@Configuration
public class FLowableConfig implements EngineConfigurationConfigurer<SpringProcessEngineConfiguration> {
    /*
     * desc: flowable配置----为放置生成的流程图中中文乱码
     */
    @Override
    public void configure(SpringProcessEngineConfiguration engineConfiguration) {
        engineConfiguration.setActivityFontName("宋体");
        engineConfiguration.setLabelFontName("宋体");
        engineConfiguration.setAnnotationFontName("宋体");
    }
}
