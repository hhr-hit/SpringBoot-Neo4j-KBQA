package com.hhr.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

	/**
	 * hanlp使用的字典的路径
	 */
	@Value("${rootDirPath}")
	private String rootDictPath;
	@Value("${HanLP.CustomDictionary.path.dianpuDict}")
	private String dianpuDictPath;
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
				+ dianpuDictPath + "、"
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
	public String answer(String question) throws Exception {

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
		 * 加载自定义的店铺字典  设置词性 dp 0
		 */
		loadDianpuDict(dianpuDictPath);
		System.out.println("加载店铺字典，设置词性dp，频率0");

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
		ArrayList<String> reStrings = queryProcess.analyQuery(question); //问句分析

		int modelIndex = Integer.valueOf(reStrings.get(0)); // 0号位 index
		System.out.println("从查询集合中提取问题分类序号：" + modelIndex);

		String answer = null;
		String name = "";
		String type = "";
		String att  = "";


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
				System.out.println("查询Neo4j数据库：bolt://localhost:7687");
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
				}
				break;
			case 1:
				/**
				 * 1 对应问题模板1 == att(参数) n(家电类型名称，如冰箱) 有哪些
				 */
				att = reStrings.get(1); //获取att
				type = reStrings.get(2); //获取n
				System.out.println("从查询集合中获取参数：" + att + " " + type);
				System.out.println("查询Neo4j数据库：bolt://localhost:7687");
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
				}
				break;
			case 2:
				/**
				 * 2 对应问题模板2 == 有哪些 cj(厂家) vsc(动词，如制造) n(家电类型名称，如冰箱)
				 */
				type = reStrings.get(4); //获取n
				System.out.println("从查询集合中获取参数：" + type);
				System.out.println("查询Neo4j数据库：bolt://localhost:7687");
				type = ".*" + type + ".*"; //模糊查询
				List<String> pinpais0 = qRepository.getPinpaiByType(type);
				if (pinpais0.size() == 0) {
					answer = null;
				} else {
					answer = pinpais0.toString().replace("[", "").replace("]", "");
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
				StringBuffer sb = new StringBuffer();
				for(int i = 1; i < reStrings.size()-2; i++) {
					sb.append(reStrings.get(i)); //获取noj 拼接
					if(i != reStrings.size()-3) {
						sb.append(" "); //加上空格
					}
				}
				name = sb.toString(); //真正的名称
				System.out.println("最终的名称参数：" + name);
				System.out.println("查询Neo4j数据库：bolt://localhost:7687");
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

				break;
			case 5:

				break;
			case 6:

				break;
			default:
				break;
		}

		System.out.println(" ");
		System.out.println("查询结束，结果为：");
		System.out.println(answer);

		/**
		 * 生成答案
		 */
		//System.out.println("根据结果生成答案：" + answer);

		if (answer != null && !answer.equals("") && !answer.equals("\\N")) {
			return answer;
		} else {
			return "sorry,我没有找到你要的答案";
		}
	}


	/**
	 * 调用addCustomDictionary()方法
	 * 加载自定义店铺字典
	 * @param path
	 */
	public void loadDianpuDict(String path) {
		File file = new File(path);
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
			addCustomDictionary(br, 0);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
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
					 * 设置
					 */
					case 7:
						CustomDictionary.add(word, "");
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
