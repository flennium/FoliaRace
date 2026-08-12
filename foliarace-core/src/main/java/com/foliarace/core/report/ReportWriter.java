package com.foliarace.core.report;

import java.io.IOException;
import java.nio.file.Path;

public interface ReportWriter {
    void write(Path destination, ReportDocument report) throws IOException;
}
