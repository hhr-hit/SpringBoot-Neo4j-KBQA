package com.hhr.controller;

import com.hhr.process.ModelProcess;
import com.hhr.service.impl.QuestionServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.hhr.service.QuestionService;
import org.neo4j.driver.v1.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/rest/hhr/question")
public class QuestionController {
	
	@Autowired
	QuestionService questService;

	@Autowired
	QuestionServiceImpl questServiceImpl;

	@Value("${rootDirPath}")
	private String rootDictPath;
	@Value("${HanLP.CustomDictionary.path.pinpaiDict}")
	private String pinpaiDictPath;
	@Value("${HanLP.CustomDictionary.path.jiadianDict}")
	private String jiadianDictPath;
	@Value("${HanLP.CustomDictionary.path.attDict}")
	private String attDictPath;
	@Value("${HanLP.CustomDictionary.path.cjDict}")
	private String cjDictPath;
	@Value("${HanLP.CustomDictionary.path.vscDict}")
	private String vscDictPath;
	@Value("${HanLP.CustomDictionary.path.nojDict}")
	private String nojDictPath;
	@Value("${HanLP.CustomDictionary.path.mqDict}")
	private String mqDictPath;

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
	 * 生成图谱
	 * 等待前端访问
	 * 无返回值
	 * 无参需数
	 * @throws IOException
	 */
    @RequestMapping("/neo")
    public void creatNeo() throws IOException {
        Runtime rn = Runtime.getRuntime();
        Process p = null;
        // 要用/做路径分隔符，而不是\
        // 需要注意捕获io异常
        p = rn.exec("cmd.exe /k start createNeo4j.bat");
        //p = rn.exec("cmd.exe /k start test01.bat");
    }

	/**
	 * 读取历史文件
	 * 按行存储
	 * 返回前台
	 * @return 历史记录
	 * @throws Exception
	 */
	@RequestMapping("/history")
	public String seeHistory() throws Exception {
		File file = new File("history.txt");
		if(!file.exists()){
			//throw new RuntimeException("要读取的文件不存在");
			return null;
		}
		BufferedReader reader = null;
		StringBuilder laststr = new StringBuilder(); //手动构造
		try {
			FileInputStream fileInputStream = new FileInputStream(file);
			InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
			reader = new BufferedReader(inputStreamReader);
			String tempString = null;
			while ((tempString = reader.readLine()) != null){
				laststr.append(tempString);
				laststr.append("<br>");
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		System.out.println("文件读取成功，内容为：" + laststr.toString());
		return laststr.toString();
	}

	/**
	 * 系统启动时
	 * 对文件排序
	 * 以便于推荐
	 */
	@RequestMapping("/recommend")
	public String recommendQuestion() throws Exception {
		int[] counts = new int[10]; //记录每个模板出现的次数
		int   max    = 0; //记录出现次数最多的序号
		String result = ""; //最终返回
		String[][] qs = new String[50][3]; //记录每行
		int k = 0; //计数

		File filer = new File("history.txt"); //读文件
		if(!filer.exists()){ //读文件
			return null; //读文件
		} //读文件
		BufferedReader reader = null; //读文件
		try {
			FileInputStream fileInputStream = new FileInputStream(filer); //读文件
			InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8"); //读文件
			reader = new BufferedReader(inputStreamReader); //读文件
			String tempString = null; //读文件
			while ((tempString = reader.readLine()) != null){ //读文件
				String[] strArr = tempString.split(","); //以,分割每一行
				qs[k][0] = strArr[0]; //序号
				qs[k][1] = strArr[2]; //问句
				k++;
				switch (strArr[0]){ //计数
					case "0.0":
						counts[0]++; //计数
						break;
					case "1.0":
						counts[1]++;
						break;
					case "2.0":
						counts[2]++;
						break;
					case "3.0":
						counts[3]++;
						break;
					case "4.0":
						counts[4]++;
						break;
					case "5.0":
						counts[5]++;
						break;
					case "6.0":
						counts[6]++;
						break;
					case "7.0":
						counts[7]++;
						break;
					case "8.0":
						counts[8]++;
						break;
					case "9.0":
						counts[9]++;
						break;
					default :
						break;
				} //计数结束
			} //读取结束
			reader.close(); //关闭文件
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		int aar_Max = counts[0],aar_index=0;
		for(int i=0;i<counts.length;i++){
			if(counts[i]>aar_Max){//比较后赋值
				aar_Max=counts[i];
				aar_index = i;
			}
		}
		System.out.println("出现次数最多的模板： "+aar_index);
		System.out.println("出现次数： "+aar_Max);

		max = aar_index;
		for (int i=0; i<k; ++i) { //匹配
			if(qs[i][0].equals(max+".0")){ //是出现最多的
				result = qs[i][1]; //将问句内容返回
				break;
			}
		} //获得出现最多的问句内容

		return result;
	}

	/**
	 * 系统启动时
	 * 提前加载问题模板和字典
	 */
	@RequestMapping("/onload")
	public String loadDictAndPattern() throws Exception {
		//List<String> list = questService.answer("预加载");
		ModelProcess queryProcess = new ModelProcess(rootDictPath);
		System.out.println("开始加载字典");
		questServiceImpl.loadMqDict(mqDictPath);
		questServiceImpl.loadNojDict(nojDictPath);
		questServiceImpl.loadVscDict(vscDictPath);
		questServiceImpl.loadCjDict(cjDictPath);
		questServiceImpl.loadAttDict(attDictPath);
		questServiceImpl.loadPinpaiDict(pinpaiDictPath);
		questServiceImpl.loadJiadianDict(jiadianDictPath);
		System.out.println("字典加载成功");
		return "扩展字典预加载成功，问题模板预训练成功";
	}

	/**
	 * 查看字典路径
	 */
	@RequestMapping("/path")
	public void checkPath(){
		questService.showDictPath();
	}
}
