package com.moldtrial.domain;

public enum RetestResult {
    PASS, // 复测通过（但仅在适用参数范围内才允许关闭问题）
    FAIL, // 复测失败：回到整改，失败记录保留
    CONDITION_MISMATCH // 样件本身合格但模具版次/工艺窗口不匹配，不能证明旧问题解决
}
