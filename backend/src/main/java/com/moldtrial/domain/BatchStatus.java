package com.moldtrial.domain;

public enum BatchStatus {
    IN_PROGRESS, // 试模进行中
    PASSED,      // 已发布通过结论（全部适用的严重问题关闭后才允许）
    REJECTED
}
