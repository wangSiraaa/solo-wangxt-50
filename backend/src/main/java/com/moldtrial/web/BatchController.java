package com.moldtrial.web;

import com.moldtrial.api.Dtos.BatchDetail;
import com.moldtrial.api.Dtos.BatchListItem;
import com.moldtrial.api.Dtos.ReleaseBatchRequest;
import com.moldtrial.service.BatchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/batches")
public class BatchController {

    private final BatchService service;

    public BatchController(BatchService service) {
        this.service = service;
    }

    @GetMapping
    public List<BatchListItem> list() {
        return service.list();
    }

    @GetMapping("/{code}")
    public BatchDetail detail(@PathVariable String code) {
        return service.detail(code);
    }

    /** 发布通过结论：全部适用严重问题关闭后才允许，否则 422 并列出阻断项 */
    @PostMapping("/{code}/release")
    public BatchDetail release(@PathVariable String code, @RequestBody ReleaseBatchRequest req) {
        return service.release(code, req);
    }
}
