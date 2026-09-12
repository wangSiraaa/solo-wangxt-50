package com.moldtrial.domain;

public enum IssueStatus {
    OPEN,          // 待处理
    IN_RECTIFY,    // 整改中（复测失败后回到整改）
    CLOSED         // 已关闭（必须由适用范围内通过复测确认）
}
