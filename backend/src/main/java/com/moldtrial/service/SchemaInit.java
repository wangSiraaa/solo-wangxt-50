package com.moldtrial.service;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 在 Hibernate 建表之后、种子数据之前，为只追加表创建数据库级触发器。
 * 用 JDBC 整句执行（而非 schema.sql 按分号切分），plpgsql 函数体内的分号不会被截断。
 *
 * retest / rectification_record 一旦写入即不可 UPDATE/DELETE：
 * 复测失败回到整改时，旧失败记录在数据库层面也无法被覆盖。
 */
@Component
public class SchemaInit {

    private static final Logger log = LoggerFactory.getLogger(SchemaInit.class);

    @PersistenceContext
    private EntityManager em;

    @PostConstruct
    @Transactional
    public void init() {
        em.createNativeQuery("""
                CREATE OR REPLACE FUNCTION moldtrial_block_mutation() RETURNS trigger AS $$
                BEGIN
                    RAISE EXCEPTION '表 % 为只追加记录，禁止 % 操作（复测/整改历史不可篡改）',
                        TG_TABLE_NAME, TG_OP;
                END;
                $$ LANGUAGE plpgsql
                """).executeUpdate();

        install("retest", "trg_retest_immutable");
        install("rectification_record", "trg_rectification_immutable");
        log.info("只追加触发器已安装：retest, rectification_record");
    }

    private void install(String table, String trigger) {
        em.createNativeQuery("DROP TRIGGER IF EXISTS " + trigger + " ON " + table).executeUpdate();
        em.createNativeQuery("""
                CREATE TRIGGER %s
                    BEFORE UPDATE OR DELETE ON %s
                    FOR EACH ROW EXECUTE FUNCTION moldtrial_block_mutation()
                """.formatted(trigger, table)).executeUpdate();
    }
}
