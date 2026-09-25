package com.hackathon.platform.dto;

/** One source file's raw content, as submitted, for rendering in the admin diff view. */
public record SourceFileResponse(String fileName, String content) {}
