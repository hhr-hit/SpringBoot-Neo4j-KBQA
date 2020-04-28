package com.hhr.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.hhr.service.QuestionService;

@RestController
@RequestMapping("/rest/hhr/question")
public class QuestionController {
	
	@Autowired
	QuestionService questService;

	/**
	 * 问答查询的控制器
	 * @param question问句
	 * @return 答案
	 * @throws Exception
	 */
	@RequestMapping("/query")
	public String query(@RequestParam(value = "question") String question) throws Exception {

		//System.out.println("用户在前端提交的问句已收到，内容为：“" + question + "“");

		return questService.answer(question);
	}

	/**
	 * 查看字典路径
	 */
	@RequestMapping("/path")
	public void checkPath(){
		questService.showDictPath();
	}
}
