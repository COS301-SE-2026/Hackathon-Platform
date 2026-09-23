package com.hackathon.platform.ide;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.platform.dto.WorkspaceRunResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class WorkspaceSubmissionPackager {
    private final WorkspaceFileStore fileStore;
    private final ObjectMapper objectMapper;

    public WorkspaceSubmissionPackage createPackage(IdeWorkspaceResources resources, WorkspaceRunResponse runResult) {
        byte[] sourceZip = createSourceZip(resources);
        byte[] outputJson = createOutputJson(runResult);

        return new WorkspaceSubmissionPackage(sourceZip, outputJson);
    }

    private byte[] createSourceZip(IdeWorkspaceResources resources) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();

            try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
                var files = fileStore.listFiles(resources);
                for (var file : files) {
                    if (file.directory()) {
                        continue;
                    }

                    String content = fileStore.readFile(resources, file.path());

                    ZipEntry entry = new ZipEntry(file.path());
                    zip.putNextEntry(entry);
                    zip.write(content.getBytes(StandardCharsets.UTF_8));

                    zip.closeEntry();
                }
            }
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not create zip", e);
        }
    }

    private byte[] createOutputJson(WorkspaceRunResponse runResult) {
        if (!runResult.success()) {
            throw new IllegalStateException("Execution failed, abort submission: " + runResult.error());
        }

        String output = runResult.output();
        if (output == null || output.isBlank()) {
            throw new IllegalStateException("Workspace produced no output");
        } 

        try {
            objectMapper.readTree(output);
            return output.getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Program output is not valid JSON", e);
        }
    }
}