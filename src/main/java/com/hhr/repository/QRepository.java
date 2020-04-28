package com.hhr.repository;

import com.hhr.node.jd_entity;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import java.util.List;

/**
 * 问答的neo4j数据库查询接口
 * CQL
 * Cypher查询语言
 * 因为图谱数据原因，用到较多的模糊查询处理
 */
public interface QRepository extends Neo4jRepository<jd_entity,Long> {

    /**
     * 0 对应问题模板0   ntc(品牌) n(家电类型名称，如冰箱、热水器、空调、洗衣机、电视机)
     * @param ntc 品牌名
     * @param n 家电类型名称
     * @return 返回家电名称列表
     */
    @Query("match(m)-[r:品牌]->(n) where n.name=~{name} "
            + "match(m)-[r:品牌]->(n) where m.name =~{type} return distinct m.name")
    List<String> getJiandianByPinpai(@Param("name") String name, @Param("type") String type);
//    @Query("match(m)-[r:价格]->(n) where m.name=~{name} and m.name =~{type} return n.name")
//    List<String> getJiandianpByPinpai(@Param("name") String name, @Param("type") String type);

    /**
     * 1 对应问题模板1   att(参数) n(家电类型名称，如冰箱)
     * @param att 参数
     * @param n 家电类型名称
     * @return 返回家电名称列表
     */
    @Query("match(m)-[r:价格]->(n) where m.name=~{att} "
            + "match(m)-[r:价格]->(n) where m.name =~{type} return distinct m.name")
    List<String> getJiandianByAtt(@Param("att") String att, @Param("type") String type);
//    @Query("match(m)-[r:价格]->(n) where m.name=~{att} "
//            + "match(m)-[r:价格]->(n) where m.name =~{type} return n.name")
//    List<String> getJiandianpByAtt(@Param("att") String att, @Param("type") String type);

    /**
     * 2 对应问题模板2   有哪些 cj(如厂家) vsc(如生产) n(家电类型名称，如冰箱)
     * @param type 家电类型名称
     * @return 返回品牌(厂家)名称列表
     */
    @Query("match(m)-[r:品牌]->(n) where m.name =~{type} return distinct n.name")
    List<String> getPinpaiByType(@Param("type") String type);

    /**
     * 3 对应问题模板3   noj(家电具体名称，如某某某某冰箱) 价格 是多少
     * @param name 家电类型名称
     * @return 返回价格
     */
    @Query("match(m)-[r:价格]->(n) where m.name =~{name} return n.name")
    Double getPriceByName(@Param("name") String name);

    /**
     * 4 对应问题模板4   ntc vsc 哪种家电
     * @param type 家电类型名称
     * @return 返回家电名称列表
     */


}