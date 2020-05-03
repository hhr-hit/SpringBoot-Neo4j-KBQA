package com.hhr.controller;

//import net.sf.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.hhr.service.QuestionService;

import java.util.ArrayList;
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
	 * 查看字典路径
	 */
	@RequestMapping("/path")
	public void checkPath(){
		questService.showDictPath();
	}
}
