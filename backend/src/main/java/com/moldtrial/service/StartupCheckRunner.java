package com.moldtrial.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 启动后自动跑一次一致性自检，结果写入日志（确保在种子数据 @Order(0) 之后执行） */
@Component
public class StartupCheckRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupCheckRunner.class);
    private final ConsistencyService consistency;

    public StartupCheckRunner(ConsistencyService consistency) {
        this.consistency = consistency;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(100)
    public void run() {
        var report = consistency.check();
        log.info("==== 启动数据一致性检查：{} ====", report.healthy() ? "通过 ✅" : "发现问题 ❌");
        report.checks().forEach(c -> log.info("  [{}] {} — {}", c.ok() ? "OK" : "FAIL", c.name(), c.detail()));
    }
}
