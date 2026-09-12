package com.moldtrial.service;

/** 请求与业务规则冲突（如：无条件合格复测却关闭、批次仍有严重问题） */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
