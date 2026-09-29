package com.hackathon.platform.ide;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hackathon.java-runner")
public record EcsJavaRunnerProperties(String cluster, String taskDefinition, List<String> subnets, List<String> securityGroups){}