package com.library.kg.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

/**
 * 知识图谱 Schema 初始化器.
 * <p>
 * 应用启动后幂等创建 Neo4j 唯一约束，确保节点 ID 策略（Book.id / Author.name /
 * Keyword.name / Subject.name）的写入幂等性。
 * {@code CREATE CONSTRAINT IF NOT EXISTS} 语义保证重复执行无副作用。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KgSchemaInitializer implements ApplicationRunner {

    private final Neo4jClient neo4jClient;
    private final KnowledgeGraphProperties kgProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (!kgProperties.isConstraintsAutoCreate()) {
            log.info("kg.constraints-auto-create=false，跳过唯一约束创建");
            return;
        }
        try {
            createConstraint("Book", "id", "kg_book_id_unique");
            createConstraint("Author", "name", "kg_author_name_unique");
            createConstraint("Keyword", "name", "kg_keyword_name_unique");
            createConstraint("Subject", "name", "kg_subject_name_unique");
            log.info("Neo4j 唯一约束初始化完成");
        } catch (Exception e) {
            log.error("Neo4j 唯一约束创建失败，KG 功能可能异常: {}", e.getMessage());
        }
    }

    /**
     * 创建唯一约束（Cypher 字符串拼接）.
     * <p>
     * 安全性说明：当前所有调用方传入的 {@code label}/{@code property}/{@code constraintName}
     * 均为本类内硬编码常量（如 {@code "Book"、"id"、"kg_book_id_unique"}），
     * 不存在用户输入，因此无 Cypher 注入风险。
     * <b>若未来改为动态参数（如从配置/请求读取），必须使用白名单校验标签名与属性名。</b>
     */
    private void createConstraint(String label, String property, String constraintName) {
        String cypher = "CREATE CONSTRAINT " + constraintName
                + " IF NOT EXISTS FOR (n:" + label + ") REQUIRE n." + property + " IS UNIQUE";
        neo4jClient.query(cypher).run();
        log.debug("唯一约束 [{}] 已就位", constraintName);
    }
}
