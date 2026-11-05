package com.changde.trace.api;

import com.changde.trace.trace.TraceModels;
import com.changde.trace.trace.TraceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/trace")
public class PublicTraceController {
    private final TraceService traces;

    public PublicTraceController(TraceService traces) {
        this.traces = traces;
    }

    @GetMapping("/{traceCode}")
    public TraceModels.PublicTrace trace(@PathVariable String traceCode) {
        return traces.publicTrace(traceCode);
    }
}
