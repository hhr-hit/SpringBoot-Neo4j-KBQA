package com.hhr.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.hhr.service.QuestionService;
import org.neo4j.driver.v1.*;
import java.util.List;

@RestController
@RequestMapping("/rest/hhr/question")
public class QuestionController {
	
	@Autowired
	QuestionService questService;

	/**
	 * 问答查询的控制器
	 * @param question 问句
	 * @return 答案
	 * @throws Exception
	 */
	@RequestMapping("/query")
	public String query(@RequestParam(value = "question") String question) throws Exception {
		//System.out.println("用户在前端提交的问句已收到，内容为：“" + question + "“");
		List<String> list = questService.answer(question);

		StringBuilder jsonBuilder = new StringBuilder("{"); //手动构造json字符串
		jsonBuilder.append("\"answer\"").append(":\"").append(list.get(0));
		jsonBuilder.append("\",");
		jsonBuilder.append("\"process\"").append(":\"").append(list.get(1));
		jsonBuilder.append("\"}");
		//System.out.println(jsonBuilder.toString());

		//JSONArray listArray=JSONArray.fromObject(list);
		//JSONArray js = JSONArray.fromObject(list);
		//String  result  =  js.toString();

		return jsonBuilder.toString();
	}

	/**
	 * 用户自助查询
	 * @param cypher 查询语句
	 * @return 结果
	 * @throws Exception
	 */
	@RequestMapping("/cql")
	public String cql(@RequestParam(value = "cypher") String cypher) throws Exception {
		System.out.println("用户在前端输入的查询语句已收到，内容为：" + cypher);
		/**
		 * 连接Neo4j
		 */
		Driver driver = GraphDatabase.driver( "bolt://localhost:7687", AuthTokens.basic( "neo4j", "2020" ) );
		Session session = driver.session(); //连接

		StatementResult result = session.run(cypher); //查询
		StringBuilder sb = new StringBuilder(); //手动构造

		String str = result.toString();
		System.out.println("");
		System.out.println("str：" + str);
		//str = "数据库中暂无此项！";

		while ( result.hasNext() ){
			Record record = result.next();
			sb.append(record.get("m.name").toString() + "<br>");
		}
		System.out.println("");
		System.out.println("sb：" + sb.toString());

		session.close();
		driver.close();

		return sb.toString();
	}

	/**
	 * 查看字典路径
	 */
	@RequestMapping("/path")
	public void checkPath(){
		questService.showDictPath();
	}
}
