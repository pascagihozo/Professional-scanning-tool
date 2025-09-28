package com.lci.scannerdesktop.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ScanPolicy {

    @Value("${api.max-result-size-bytes:52428800}")
    private long maxResultSizeBytes;

    public long getMaxResultSizeBytes() {
        return maxResultSizeBytes;
    }
}


