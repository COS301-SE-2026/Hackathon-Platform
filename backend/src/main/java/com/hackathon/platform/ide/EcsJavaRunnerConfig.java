package com.hackathon.platform.ide;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EcsJavaRunnerProperties.class)
public class EcsJavaRunnerConfig {}