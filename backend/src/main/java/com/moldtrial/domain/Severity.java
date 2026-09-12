package com.moldtrial.domain;

public enum Severity {
    CRITICAL, // 严重：适用范围内未关闭则批次不能发布
    MAJOR,    // 主要：提示但不阻断发布
    MINOR
}
