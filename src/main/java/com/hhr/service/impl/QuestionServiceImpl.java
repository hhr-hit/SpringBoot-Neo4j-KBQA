package com.hhr.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hhr.repository.QRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import com.hhr.process.ModelProcess;
import com.hhr.service.QuestionService;
import com.hankcs.hanlp.dictionary.CustomDictionary;

/**
 * 服务层实现
 * 读取文件，追加自定义字典CustomDictionary
 * 调用数据库连接层与处理函数，实现问答业务逻辑
 */
@Service
@Primary
public class QuestionServiceImpl implements QuestionService {

	StringBuffer process = new StringBuffer(); //处理流程

	/**
	 * hanlp使用的字典的路径
	 */
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
	 * 声明neo4j查询接口对象
	 */
	@Autowired
	private QRepository qRepository;

	/**
	 * 实现showDictPath方法
	 * 输出字典路径，仅做调试用
	 * 检查整个系统中，字典的路径是否写对
	 */
	@Override
	public void showDictPath() {
		System.out.println("HanLP分词字典及自定义问题模板根目录：" + rootDictPath);
		System.out.println("用户自定义扩展词库："
				+ pinpaiDictPath + "、"
				+ jiadianDictPath);
	}

	/**
	 * 实现answer()方法
	 * 调用ModelProcess中定义好的方法
	 * 输入问句，处理后，返回答案
	 * @param question
	 * @return 返回要传给前端的答案
	 * @throws Exception
	 */
	@Override
	public List<String> answer(String question) throws Exception {

		/**
		 * 生成可供使用的HanLP分词器与贝叶斯分类器
		 * ModelProcess.java
		 */
		ModelProcess queryProcess = new ModelProcess(rootDictPath);

		System.out.println(" ");
		System.out.println("HanLP分词字典根目录为："+ rootDictPath + "/dictionary");
		System.out.println(" ");
		System.out.println("========用户自定义扩展词库开始加载========");

		/**
		 * 加载自定义的参数属性字典  设置词性 mq 1000
		 * 1000防止冲突，优先自定义词性
		 */
		loadMqDict(mqDictPath);
		System.out.println("加载参数属性字典，设置词性mq，频率0");

		/**
		 * 加载自定义的名称字典  设置词性 noj 0
		 */
		loadNojDict(nojDictPath);
		System.out.println("加载名称字典，设置词性noj，频率0");

		/**
		 * 加载自定义的动词字典  设置词性 vsc 0
		 */
		loadVscDict(vscDictPath);
		System.out.println("加载动词字典，设置词性vsc，频率0");

		/**
		 * 加载自定义的厂家字典  设置词性 cj 0
		 */
		loadCjDict(cjDictPath);
		System.out.println("加载厂家字典，设置词性cj，频率0");

		/**
		 * 加载自定义的参数字典  设置词性 att 0
		 */
		loadAttDict(attDictPath);
		System.out.println("加载参数字典，设置词性att，频率0");

		/**
		 * 加载自定义的品牌字典  设置词性 ntc 0
		 */
		loadPinpaiDict(pinpaiDictPath);
		System.out.println("加载品牌字典，设置词性ntc，频率0");

		/**
		 * 加载自定义的家电字典  设置词性 n 0
		 */
		loadJiadianDict(jiadianDictPath);
		System.out.println("加载家电种类字典，设置词性n，频率0");

		System.out.println("========用户自定义字典加载完毕========");


		/**
		 * 调用ModelProcess的analyQuery()方法
		 * analyQuery()方法是具体业务的总入口
		 * 分析问句，返回 index与分词
		 * 设置语句处理参数
		 * 临时变量
		 * 提取问题分类序号
		 * 提取分词，作为参数调用QRepository方法，查询数据库
		 */
		ArrayList<String> reStrings = queryProcess.analyQuery(question); //问句分析，得到查询集合

		process.append(reStrings.get(reStrings.size() - 1)); //提取处理流程，保存
		reStrings.remove(reStrings.size() - 1); //删除处理流程

		int modelIndex = Integer.valueOf(reStrings.get(0)); // 0号位 index

		System.out.println(" ");
		System.out.println("从查询集合中提取问题分类序号：" + modelIndex);
		System.out.println(" ");
		process.append("从查询集合中提取问题分类序号：" + modelIndex + "<br><br>");

		String answer = null;
		String name = "";
		String type = "";
		String att  = "";
		String mq  = "";


		/**
		 * 匹配问题模板序号
		 * 调用对应的查询接口
		 */
		switch (modelIndex) {
			case 0:
				/**
				 * 0 对应问题模板0 == ntc(品牌，如海尔) n(家电类型名称，如冰箱、热水器、空调、洗衣机、电视机)
				 */
				name = reStrings.get(1); //获取ntc
				type = reStrings.get(2); //获取n

				System.out.println("从查询集合中获取参数：" + name + " " + type);
				System.out.println(" ");
				System.out.println("查询Neo4j数据库：bolt://localhost:7687");
				process.append("从查询集合中获取参数：" + name + " " + type + "<br><br>"
								+ "查询Neo4j数据库：bolt://localhost:7687" + "<br><br>");

				name = ".*" + name + ".*"; //模糊查询
				type = ".*" + type + ".*";
				List<String> jiadians0 = qRepository.getJiandianByPinpai(name, type); //名字
				//List<String> jiadians00 = qRepository.getJiandianpByPinpai(name, type); //价格
				//System.out.println(jiadians00); //输出价格
				if (jiadians0.size() == 0) {
					answer = null;
				} else {
//					List<String> jiadians000 = new ArrayList();
//					for(int i=0; i<jiadians0.size(); i++){ //合并、拼接后，返回名字和价格
//						jiadians000.add(jiadians0.get(i));
//						jiadians000.add(jiadians00.get(i));
//					}
//					answer = jiadians000.toString().replace("[", "").replace("]", "");
					answer = jiadians0.toString().replace("[", "").replace("]", "");
					StringBuilder sb = new StringBuilder();
					for(String x:jiadians0){
						sb.append(x);
						sb.append("<br>");
					}
					answer = sb.toString();
				}
				break;

			case 1:
				/**
				 * 1 对应问题模板1 == att(参数) n(家电类型名称，如冰箱) 有哪些
				 */
				att = reStrings.get(1); //获取att
				type = reStrings.get(2); //获取n

				System.out.println("从查询集合中获取参数：" + att + " " + type);
				System.out.println(" ");
				System.out.println("查询Neo4j数据库：bolt://localhost:7687");
				process.append("从查询集合中获取参数：" + att + " " + type + "<br><br>"
						+ "查询Neo4j数据库：bolt://localhost:7687" + "<br><br>");
				att = ".*" + att + ".*"; //模糊查询
				type = ".*" + type + ".*";
				//String tq = ".*" + att + ".*" + "&" + ".*" + type + ".*";
				//List<String> jiadians1 = qRepository.getJiandianByAtt(tq);
				List<String> jiadians1 = qRepository.getJiandianByAtt(att, type);
				//List<String> jiadians11 = qRepository.getJiandianpByAtt(att, type);
				//System.out.println(jiadians11); //输出价格
				if (jiadians1.size() == 0) {
					answer = null;
				} else {
//					List<String> jiadians111 = new ArrayList();
//					for(int i=0; i<jiadians1.size(); i++){ //返回名字和价格
//						jiadians111.add(jiadians1.get(i));
//						jiadians111.add(jiadians11.get(i));
//					}
					answer = jiadians1.toString().replace("[", "").replace("]", "");
					StringBuilder sb = new StringBuilder();
					for(String x:jiadians1){
						sb.append(x);
						sb.append("<br>");
					}
					answer = sb.toString();
				}
				break;

			case 2:
				/**
				 * 2 对应问题模板2 == 有哪些 cj(厂家) vsc(动词，如制造) n(家电类型名称，如冰箱)
				 */
				type = reStrings.get(4); //获取n

				System.out.println("从查询集合中获取参数：" + type);
				System.out.println(" ");
				System.out.println("查询Neo4j数据库：bolt://localhost:7687");
				process.append("从查询集合中获取参数：" + type + "<br><br>"
						+ "查询Neo4j数据库：bolt://localhost:7687" + "<br><br>");

				type = ".*" + type + ".*"; //模糊查询
				List<String> pinpais0 = qRepository.getPinpaiByType(type);
				if (pinpais0.size() == 0) {
					answer = null;
				} else {
					answer = pinpais0.toString().replace("[", "").replace("]", "");
					StringBuilder sb = new StringBuilder();
					for(String x:pinpais0){
						sb.append(x);
						sb.append("<br>");
					}
					answer = sb.toString();
				}
				break;

			case 3:
				/**
				 * 3 对应问题模板3 == noj(家电具体名称，如某某某某冰箱) 价格 是多少
				 * 处理算法：
				 * 获取reStrings大小，跳过 第一个位置(index) 和 最后两个位置(价格 是多少)
				 * 剩下的就是 家电具体名称 ，将空格加在中间，因为名字中有空格，划分模板的时候会一并分隔开
				 * 这样处理将其精确提取
				 */
				System.out.println("处理家电名称，拼接为：");
				StringBuffer sb3 = new StringBuffer();
				for(int i = 1; i < reStrings.size()-2; i++) {
					sb3.append(reStrings.get(i)); //获取noj 拼接
					if(i != reStrings.size()-3) {
						sb3.append(" "); //加上空格
					}
				}
				name = sb3.toString(); //真正的名称
				System.out.println(name);
				System.out.println(" ");
				System.out.println("查询Neo4j数据库：bolt://localhost:7687");
				process.append("处理家电名称，拼接为：<br>" + name + "<br><br>"
						+ "查询Neo4j数据库：bolt://localhost:7687" + "<br><br>");

				//System.out.println(name);
				//name = reStrings.get(1); //获取noj
				name = ".*" + name + ".*"; //模糊查询
				Double price = qRepository.getPriceByName(name);
				if (price == null) {
					answer = null;
				} else {
					answer = price.toString().replace("[", "").replace("]", "");
				}
				break;

			case 4:
				/**
				 * 4 对应问题模板4   ntc vsc 哪种 家电
				 * 返回家电名称列表，需要再次处理
				 * 子串模糊匹配
				 * 去重
				 */
				name = reStrings.get(1); //获取ntc
				System.out.println("从查询集合中获取参数：" + name);
				System.out.println(" ");
				System.out.println("查询Neo4j数据库：bolt://localhost:7687");
				process.append("从查询集合中获取参数：" + name + "<br><br>"
						+ "查询Neo4j数据库：bolt://localhost:7687" + "<br><br>");
				name = ".*" + name + ".*"; //模糊查询
				List<String> jiadians4 = qRepository.getAllnByNtc(name); //名字
				StringBuilder sb4 = new StringBuilder(); //手动构造答案

				Pattern p1 = Pattern.compile("冰箱");
				for(String x:jiadians4){
					Matcher m1 = p1.matcher(x);
					if(m1.find()){
						sb4.append("冰箱");
						sb4.append("<br>");
						break;
					}
				}
				Pattern p2 = Pattern.compile("洗衣机");
				for(String x:jiadians4){
					Matcher m2 = p2.matcher(x);
					if(m2.find()){
						sb4.append("洗衣机");
						sb4.append("<br>");
						break;
					}
				}
				Pattern p3 = Pattern.compile("电视机");
				for(String x:jiadians4){
					Matcher m3 = p3.matcher(x);
					if(m3.find()){
						sb4.append("电视机");
						sb4.append("<br>");
						break;
					}
				}
				Pattern p4 = Pattern.compile("空调");
				for(String x:jiadians4){
					Matcher m4 = p4.matcher(x);
					if(m4.find()){
						sb4.append("空调");
						sb4.append("<br>");
						break;
					}
				}
				Pattern p5 = Pattern.compile("热水器");
				for(String x:jiadians4){
					Matcher m5 = p5.matcher(x);
					if(m5.find()){
						sb4.append("热水器");
						sb4.append("<br>");
						break;
					}
				}
				answer = sb4.toString();
				break;

			case 5:
				/**
				 * 5 对应问题模板5   noj 所有参数
				 * 获取reStrings大小，跳过 第一个位置0(index) 和 最后一个位置(所有参数)
				 */
				System.out.println("处理家电名称，拼接为：");
				StringBuffer sb5 = new StringBuffer();
				for(int i = 1; i < reStrings.size()-1; i++) {
					sb5.append(reStrings.get(i)); //获取noj 拼接
					if(i != reStrings.size()-2) {
						sb5.append(" "); //加上空格
					}
				}
				name = sb5.toString(); //真正的名称
				System.out.println(name);
				System.out.println("");
				System.out.println("查询Neo4j数据库：bolt://localhost:7687");
				process.append("处理家电名称，拼接为：<br>" + name + "<br><br>"
						+ "查询Neo4j数据库：bolt://localhost:7687" + "<br><br>");
				//name = ".*" + name + ".*"; //模糊查询
				List<String> all = qRepository.getAllByNoj(name);
				if (all == null) {
					answer = null;
				} else {
					//answer = all.toString().replace("[", "").replace("]", "");
					StringBuilder sb = new StringBuilder();
					for(String x:all){
						sb.append(x);
						//System.out.println(x);
						sb.append("<br>");
					}
					//加入购买推荐链接
					sb.append("购买：https://search.jd.com/Search?keyword=" + name);
					answer = sb.toString();
				}
				break;

			case 6:
				/**
				 * 6 对应问题模板6   noj mq 是多少
				 * 获取reStrings大小，跳过 第一个位置0(index) 和 最后两个位置(mq 是多少)
				 */
				System.out.println("处理家电名称，拼接为：");
				StringBuffer sb6 = new StringBuffer();
				for(int i = 1; i < reStrings.size()-2; i++) {
					sb6.append(reStrings.get(i)); //获取noj 拼接
					if(i != reStrings.size()-3) {
						sb6.append(" "); //加上空格
					}
				}
				name = sb6.toString(); //真正的名称
				mq  = reStrings.get(reStrings.size()-2); //倒数第二个位置
				System.out.println(name);
				System.out.println("要查询的属性名：" + mq);
				System.out.println("");
				System.out.println("查询Neo4j数据库：bolt://localhost:7687");
				process.append("处理家电名称，拼接为：<br>" + name
						+ "<br>" + "要查询的属性名：" + mq + "<br><br>"
						+ "查询Neo4j数据库：bolt://localhost:7687" + "<br><br>");
				name = ".*" + name + ".*"; //模糊查询
				mq = ".*" + mq + ".*"; //模糊查询
				String mqvalue = qRepository.getMcsByNoj(name,mq);
				if (mqvalue == null) {
					answer = null;
				} else {
					answer = mqvalue.toString().replace("[", "").replace("]", "");
				}
				break;

			default:
				break;
		}

		System.out.println(" ");
		System.out.println("查询结束，结果为：");
		System.out.println(answer);
		process.append("查询结束，结果为：" + "<br>" + answer + "<br><br>"); //处理结束

		/**
		 * 生成答案
		 */
		//System.out.println("根据结果生成答案：" + answer);
		List<String> res = new ArrayList<String>();
		if (answer != null && !answer.equals("") && !answer.equals("\\N")) {
			res.add(answer); //得到答案
		} else {
			res.add("sorry,我没有找到你要的答案");
		}
		res.add(process.toString()); //加入处理过程
		process.setLength(0); //清空

		return res; //答案 //处理过程
	}


	/**
	 * 加载自定义品牌字典
	 * @param path
	 */
	public void loadPinpaiDict(String path) {
		File file = new File(path);
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			addCustomDictionary(br, 1);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
	}


	/**
	 * 加载自定义家电字典
	 * @param path
	 */
	public void loadJiadianDict(String path) {
		File file = new File(path);
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			addCustomDictionary(br, 2);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
	}


	/**
	 * 加载自定义参数字典
	 * @param path
	 */
	public void loadAttDict(String path) {
		File file = new File(path);
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			addCustomDictionary(br, 3);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
	}


	/**
	 * 加载自定义厂家字典
	 * @param path
	 */
	public void loadCjDict(String path) {
		File file = new File(path);
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			addCustomDictionary(br, 4);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
	}


	/**
	 * 加载自定义动词字典
	 * @param path
	 */
	public void loadVscDict(String path) {
		File file = new File(path);
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			addCustomDictionary(br, 5);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
	}


	/**
	 * 加载自定义家电具体名称字典
	 * @param path
	 */
	public void loadNojDict(String path) {
		File file = new File(path);
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			addCustomDictionary(br, 6);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * 加载某参数字典
	 * @param path
	 */
	public void loadMqDict(String path) {
		File file = new File(path);
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			addCustomDictionary(br, 7);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * 供loadDict()函数调用
	 * 往用户字典中添加自定义分词及其词性
	 * 数字0表示频率
	 * @param br
	 * @param type
	 */
	public void addCustomDictionary(BufferedReader br, int type) {
		String word;
		try {
			while ((word = br.readLine()) != null) {
				//System.out.println(new String(word.getBytes("utf-8")));
				switch (type) {
					/**
					 * 设置店铺 词性 == dp 0
					 */
					case 0:
						CustomDictionary.add(word, "dp 0");
						break;
					/**
					 * 设置品牌 词性 == ntc 0
					 */
					case 1:
						CustomDictionary.add(word, "ntc 0");
						break;
					/**
					 * 设置家电 词性 == n 0
					 */
					case 2:
						CustomDictionary.add(word, "n 0");
						break;
					/**
					 * 设置参数 词性 == att 0
					 */
					case 3:
						CustomDictionary.add(word, "att 0");
						break;
					/**
					 * 设置厂家名词 词性 == cj 0
					 */
					case 4:
						CustomDictionary.add(word, "cj 0");
						break;
					/**
					 * 设置动词 词性 == vsc 0
					 */
					case 5:
						CustomDictionary.add(word, "vsc 0");
						break;
					/**
					 * 设置名称 词性 == noj 0
					 */
					case 6:
						CustomDictionary.add(word, "noj 0");
						break;
					/**
					 * 设置某参数 词性 == mq 1000
					 */
					case 7:
						CustomDictionary.add(word, "mq 1000");
						break;
					default:
						break;
				}
			}
			br.close();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
