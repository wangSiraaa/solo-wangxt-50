package com.moldtrial.service;

/** 并发修改冲突（乐观锁版本过期）→ HTTP 409 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
