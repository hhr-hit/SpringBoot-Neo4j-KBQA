package com.hhr.service;

import java.util.List;

public interface QuestionService {

	void showDictPath();

	List<String> answer(String question) throws Exception;

}
