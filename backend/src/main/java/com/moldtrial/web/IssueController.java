package com.moldtrial.web;

import com.moldtrial.api.Dtos.*;
import com.moldtrial.service.IssueService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueService service;

    public IssueController(IssueService service) {
        this.service = service;
    }

    @GetMapping
    public List<IssueSummary> list() {
        return service.list();
    }

    @GetMapping("/{code}")
    public IssueDetail detail(@PathVariable String code) {
        return service.detail(code);
    }

    /** 登记整改（只追加；可更新问题的适用版次/工艺窗口） */
    @PostMapping("/{code}/rectifications")
    public IssueDetail rectify(@PathVariable String code, @RequestBody RectificationRequest req) {
        return service.addRectification(code, req);
    }

    /** 登记复测（只追加）；PASS 但条件越界会被系统记为 CONDITION_MISMATCH */
    @PostMapping("/{code}/retests")
    public IssueDetail retest(@PathVariable String code, @RequestBody RetestRequest req) {
        return service.registerRetest(code, req);
    }

    /** 关闭问题：必须存在适用范围内 PASS 复测；并发关闭后到者 409 */
    @PostMapping("/{code}/close")
    public IssueDetail close(@PathVariable String code, @RequestBody CloseIssueRequest req) {
        return service.close(code, req);
    }
}
