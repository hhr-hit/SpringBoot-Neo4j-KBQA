import org.junit.Test;
import org.neo4j.driver.v1.*;

public class CypherTest {
    @Test
    public void TestA(){
        Driver driver = GraphDatabase.driver( "bolt://localhost:7687", AuthTokens.basic( "neo4j", "2020" ) );
        Session session = driver.session(); //连接

        StatementResult result = session.run("MATCH (m)-[r:`品牌`]->(n) where n.name=~\".*海尔.*\"  RETURN m.name LIMIT 25"); //查询
        StringBuilder sb = new StringBuilder(); //手动构造

        while ( result.hasNext() ){
            Record record = result.next();
            sb.append(record.get("m.name").toString() + "<br>");
        }
        System.out.println("");
        System.out.println("sb：" + sb.toString());

        session.close();
        driver.close();

        System.out.println("最终集合：" + sb.toString());
    }
}
