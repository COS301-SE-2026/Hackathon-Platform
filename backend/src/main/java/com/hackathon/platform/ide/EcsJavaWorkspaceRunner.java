package com.hackathon.platform.ide;

import com.hackathon.platform.dto.WorkspaceRunResponse;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.AssignPublicIp;
import software.amazon.awssdk.services.ecs.model.ContainerOverride;
import software.amazon.awssdk.services.ecs.model.DescribeTasksRequest;
import software.amazon.awssdk.services.ecs.model.DescribeTasksResponse;
import software.amazon.awssdk.services.ecs.model.LaunchType;
import software.amazon.awssdk.services.ecs.model.NetworkConfiguration;
import software.amazon.awssdk.services.ecs.model.RunTaskRequest;
import software.amazon.awssdk.services.ecs.model.RunTaskResponse;
import software.amazon.awssdk.services.ecs.model.TaskOverride;
import software.amazon.awssdk.services.ecs.model.Task;
import software.amazon.awssdk.services.ecs.model.StopTaskRequest;

@Component
@RequiredArgsConstructor
public class EcsJavaWorkspaceRunner implements WorkspaceCodeRunner {
    private static final long PROVISION_TIMEOUT_SECONDS = 120;
    private static final long POLL_MILLIS = 1000;
    private static final String CONTAINER_NAME = "java-runner";

    private final EcsClient ecsClient;
    private final WorkspaceFileStore fileStore;
    private final EcsJavaRunnerProperties properties;

    @Override
    public WorkspaceRunResponse run(IdeWorkspaceResources resources) {
        Objects.requireNonNull(resources, "Workspace resources required");

        String runId = UUID.randomUUID().toString();
        String workspaceRoot = "/workspace/" + resources.workspaceId();
        String runDirectory = workspaceRoot + "/.hackathon-runs/" + runId;
        String outputPath = ".hackathon-runs/" + runId + "/output.txt";
        String exitCodePath = ".hackathon-runs/" + runId + "/exit-code.txt";

        fileStore.writeFile(resources, outputPath, "");
        fileStore.writeFile(resources, exitCodePath, "-1");

        String script = buildScript(workspaceRoot, runDirectory);
        RunTaskResponse started = ecsClient.runTask(buildRunTaskRequest(script));

        if (started.failures() != null && !started.failures().isEmpty()) {
            throw new IllegalStateException("Java runner couldnt  start: " + started.failures());
        }
        if (started.tasks().isEmpty()) {
            throw new IllegalStateException("Java runner didnt return a task");
        }

        Task task = started.tasks().getFirst();
        String taskArn = task.taskArn();

        try {
            waitForTask(taskArn);
            String output = fileStore.readFile(resources, outputPath);
            int exitCode = Integer.parseInt(fileStore.readFile(resources, exitCodePath).trim());

            if (exitCode == 0) {
                return new WorkspaceRunResponse(true, 0, output, "");
            }
            return new WorkspaceRunResponse(false, exitCode, "", output);
        } finally {
            try {
                DescribeTasksResponse state = ecsClient.describeTasks(DescribeTasksRequest.builder().cluster(properties.cluster()).tasks(taskArn).build());
                if (!state.tasks().isEmpty() && !"STOPPED".equals(state.tasks().getFirst().lastStatus())) {
                    ecsClient.stopTask(StopTaskRequest.builder().cluster(properties.cluster()).task(taskArn).reason("Workspace execution finished or timed out").build());
                }
            } catch (Exception e) {}
        }
    }

    private RunTaskRequest buildRunTaskRequest(String script) {
        ContainerOverride container = ContainerOverride.builder().name(CONTAINER_NAME).command(List.of("sh", "-lc", script)).build();

        TaskOverride override = TaskOverride.builder().containerOverrides(container).build();

        NetworkConfiguration network = NetworkConfiguration.builder()
                        .awsvpcConfiguration(c -> c.subnets(properties.subnets()).securityGroups(properties.securityGroups()).assignPublicIp(AssignPublicIp.ENABLED))
                        .build();

        return RunTaskRequest.builder().cluster(properties.cluster()).taskDefinition(properties.taskDefinition()).launchType(LaunchType.FARGATE).networkConfiguration(network).overrides(override).build();
    }

    private String buildScript(String workspaceRoot, String runDirectory) {
        return "set +e; "
                + "mkdir -p '" + runDirectory + "'; "
                + "rm -rf /tmp/build; mkdir -p /tmp/build; "
                + "find '" + workspaceRoot + "' -type f -name '*.java' -not -path '" + runDirectory + "/*' -print0 "
                + "| xargs -0 javac -d /tmp/build > '" + runDirectory + "/output.txt' 2>&1; "
                + "compile=$?; "
                + "if [ $compile -ne 0 ]; then echo $compile > '" + runDirectory + "/exit-code.txt'; exit $compile; fi; "
                + "timeout 10s java -cp /tmp/build Main >> '" + runDirectory + "/output.txt' 2>&1; "
                + "code=$?; echo $code > '" + runDirectory + "/exit-code.txt'; exit $code";
    }

    private Task waitForTask(String taskArn) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(PROVISION_TIMEOUT_SECONDS);
        while (System.nanoTime() < deadline) {
            DescribeTasksResponse response = ecsClient.describeTasks(DescribeTasksRequest.builder().cluster(properties.cluster()).tasks(taskArn).build());

            if (response.tasks().isEmpty()) {
                throw new IllegalStateException("Java runner task disappeared: " + taskArn);
            }

            Task task = response.tasks().getFirst();
            if ("STOPPED".equals(task.lastStatus())) {
                return task;
            }
            try {
                Thread.sleep(POLL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Java runner wait was interrupted", e);
            }
        }

        throw new IllegalStateException("Java runner did not finish within " + PROVISION_TIMEOUT_SECONDS + " seconds");
    }
}
