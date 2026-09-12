package com.moldtrial.web;

import com.moldtrial.api.Dtos.ConsistencyReport;
import com.moldtrial.service.ConsistencyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/consistency")
public class ConsistencyController {

    private final ConsistencyService service;

    public ConsistencyController(ConsistencyService service) {
        this.service = service;
    }

    @GetMapping("/check")
    public ConsistencyReport get() {
        return service.check();
    }

    @PostMapping("/check")
    public ConsistencyReport post() {
        return service.check();
    }
}
