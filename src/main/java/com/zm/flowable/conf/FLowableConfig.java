package com.zm.flowable.conf;
import org.flowable.engine.common.impl.history.HistoryLevel;
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


    @Override
    public void configure(SpringProcessEngineConfiguration engineConfiguration) {
        //设置流程图
        engineConfiguration.setActivityFontName("宋体");
        engineConfiguration.setLabelFontName("宋体");
        engineConfiguration.setAnnotationFontName("宋体");
        engineConfiguration.setHistory(HistoryLevel.AUDIT.getKey()).buildProcessEngine();
    }
}
