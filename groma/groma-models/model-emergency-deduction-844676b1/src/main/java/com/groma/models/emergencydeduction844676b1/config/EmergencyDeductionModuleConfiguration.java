package com.groma.models.emergencydeduction844676b1.config;

import com.groma.base.engine.GromaModuleRuntime;
import com.groma.models.emergencydeduction844676b1.engine.runtime.EmergencyDeductionModuleRuntime;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 模型「应急决策推演-关联副本-1029」装配：登记运行时入口单例（Bean 名由宿主的全限定名生成器隔离，多模型零冲突）
 */
@Configuration
public class EmergencyDeductionModuleConfiguration {
    @Bean
    public GromaModuleRuntime moduleRuntime() {
        return new EmergencyDeductionModuleRuntime();
    }
}
