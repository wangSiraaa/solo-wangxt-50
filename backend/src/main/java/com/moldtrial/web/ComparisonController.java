package com.moldtrial.web;

import com.moldtrial.api.Dtos.Comparison;
import com.moldtrial.service.ComparisonService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comparison")
public class ComparisonController {

    private final ComparisonService service;

    public ComparisonController(ComparisonService service) {
        this.service = service;
    }

    /** 两轮试模对照：参数窗口差异 + 按模腔对齐的缺陷变化 */
    @GetMapping
    public Comparison compare(@RequestParam String from, @RequestParam String to) {
        return service.compare(from, to);
    }
}
