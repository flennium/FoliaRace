package com.foliarace.core.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JsonReportWriter implements ReportWriter {
    private final ObjectMapper mapper;

    public JsonReportWriter() {
        mapper = new ObjectMapper().findAndRegisterModules().enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public void write(Path destination, ReportDocument report) throws IOException {
        Path parent = destination.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        mapper.writeValue(destination.toFile(), report);
    }
}
