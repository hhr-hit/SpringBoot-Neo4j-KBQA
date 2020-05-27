package com.hhr.process;

import java.io.*;
import java.util.*;
import com.hhr.controller.QuestionController;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.mllib.classification.NaiveBayes;
import org.apache.spark.mllib.classification.NaiveBayesModel;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.mllib.regression.LabeledPoint;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;

/**
 * Spark贝叶斯分类器
 * HanLP分词器
 * 实现问题语句的抽象
 * 实现模板匹配
 * 实现关键性语句还原，获得数据库查询参数
 */
public class ModelProcess {

	String history1 = "", history2 = "";// 原句子 模板句式

	StringBuffer process = new StringBuffer(); //处理流程

	/**
	 * 读取人工构造的问题模板分类文件
	 * 构造 分类标签号和问句模板 对应的哈希表
	 * 增加问题模板需修改question_classification.txt
	 */
	Map<Double, String> questionsPattern;

	/**
	 * Spark中的朴素贝叶斯分类器
	 * 使用NaiveBayesModel
	 */
	NaiveBayesModel nbModel;

	/**
	 * 读取人工构造的词汇表
	 * 构造 词语和下标 对应的哈希表
	 * 增加问题模板需修改vocabulary.txt
	 */
	Map<String, Integer> vocabulary;

	/**
	 * 关键字与其词性的map键值对集合
	 * 在句子分词和抽象过程中，一并存储生成
	 */
	Map<String, String> abstractMap;

	/**
	 * 指定 问题question 及 字典的txt模板 所在的根目录
	 * 需增加问题模板训练集txt文件
	 */
	String rootDirPath = "myHanLP/data";

	/**
	 * 分类模板索引值
	 * 问题模板序号index
	 * 便于寻找对应查询接口
	 */
	int modelIndex = 0;

	/**
	 * 构造函数
	 * 在新建modelprocess对象时
	 * 传入参数，实例化各个处理对象
	 * questionsPattern、vocabulary、nbModel
	 * @throws Exception
	 */
	public ModelProcess() throws Exception{
		questionsPattern = loadQuestionsPattern();
		vocabulary = loadVocabulary();
		nbModel = loadClassifierModel();
	}
	public ModelProcess(String rootDirPath) throws Exception{
		this.rootDirPath = rootDirPath+'/';
		questionsPattern = loadQuestionsPattern();
		vocabulary = loadVocabulary();
		nbModel = loadClassifierModel();
	}

	/**
	 * 针对用户提交的问句
	 * 在后台进行分析
	 * @param queryString
	 * @return 返回分析好的问句
	 * 		   最终返回的格式为： 模板序号 分词1 分词2 分词3 分词4
	 * @throws Exception
	 */
	public ArrayList<String> analyQuery(String queryString) throws Exception {

		/**
		 * 打印问句
		 * 海尔的冰箱有哪些
		 */
		System.out.println(" ");
		System.out.println("系统已收到用户在前端提交的问句，开始分析该问句");
		System.out.println(" ");

		System.out.println("原始句子内容为：“" + queryString + "”"); //海尔的冰箱有哪些
		System.out.println(" ");
		process.append("原始句子内容为：“" + queryString + "”<br><br>");

		history1 = queryString; //原句子


		/**
		 * 抽象句子
		 * 利用HanLP分词
		 * 将关键字进行词性抽象
		 */
		/**
		 * 控制台输出的分词结果：
		 * 海尔/ntc
		 * 的/ude1
		 * 冰箱/n
		 * 有/vyou
		 * 哪些/ry
		 */
		System.out.println("========HanLP开始分词========");
		process.append("========HanLP开始分词========<br>");

		String abstr = queryAbstract(queryString); //分词结果
		System.out.println("句子抽象化结果：" + abstr); // ntc 的 n 有 哪些
		System.out.println(" ");
		process.append("句子抽象化结果：" + abstr + "<br><br>");



		/**
		 * 将抽象的句子与spark训练集中的模板进行匹配
		 * 拿到句子对应的模板
		 */
		//System.out.println(" ");
		System.out.println("========开始匹配问题模板========");
		process.append("========开始匹配问题模板========" + "<br>");

		String strPatt = queryClassify(abstr);

		//history2 = strPatt; //模板句式
		System.out.println("句子套用模板结果：" + strPatt); // ntc n 有哪些
		System.out.println("========问题模板匹配结束========");
		System.out.println(" ");
		process.append("句子套用模板结果：" + strPatt + "<br>"
						+ "========问题模板匹配结束========" + "<br><br>");



		/**
		 * 模板还原成句子
		 * 此时已经可以按模板位置提取参数
		 */
		String finalPattern = queryExtenstion(strPatt);

		System.out.println("原始句子替换成系统可识别的结果："+finalPattern);// 海尔 冰箱 有哪些
		System.out.println(" ");
		process.append("原始句子替换成系统可识别的结果："+finalPattern + "<br><br>");

		ArrayList<String> resultList = new ArrayList<String>();
		resultList.add(String.valueOf(modelIndex)); //列表0号位置 问题模板序号
		String[] finalPattArray = finalPattern.split(" "); //将抽象的句子以空格分割，存为list
		for (String word : finalPattArray)
			resultList.add(word);

		System.out.println("由问句分析，生成的最终查询集合：" + resultList);
		process.append("由问句分析，生成的最终查询集合：" + resultList + "<br><br>");

		resultList.add(process.toString());
		process.setLength(0); //清空

		return resultList; //模板序号 分词1 分词2 分词3 ... 分词n 处理过程
	}

	/**
	 * 句子抽象化
	 * 生成（单词，词性）哈希表
	 * HanLP分词、标注词性
	 * 将涉及到模板需要的单词换为对应词性
	 * @param querySentence 海尔的冰箱有哪些
	 * @return 返回句子抽象化结果 ntc 的 n 有 哪些
	 */
	public  String queryAbstract(String querySentence) {

		Segment segment = HanLP.newSegment().enableCustomDictionary(true); //允许自定义词典 //分词对象
		List<Term> terms = segment.seg(querySentence); //调用.seg方法进行分词

		String abstractQuery = "";
		abstractMap = new HashMap<String, String>(); //关键字与其词性的map键值对集合

		/**
		 * 分词后的结果中，有需要的词性，就在抽象结果中加入对应词性
		 * HanLP中的Term类
		 * .offset是偏移量
		 * .word是单词 海尔
		 * .nature是词性 ntc
		 * 整体输出就是 海尔/ntc
		 * 根据Term的特性进行处理
		 */
		int mCount = 0; //m 数词词性这个 词语出现的频率
		for (Term term : terms) {
			String word = term.word; //获取单词的部分，以用于存入抽象化结果和哈希表，比如 海尔
			//System.out.println(word); //调试
			String termStr = term.toString(); //整个输出，比如 海尔/ntc
			System.out.println(termStr); //分词结果输出
			process.append(termStr + "<br>");

			if (termStr.contains("ntc")) { //ntc 品牌
				abstractQuery += "ntc "; //存入 抽象化结果 字符串
				abstractMap.put("ntc", word); //生成 关键字，词性 的哈希表
			} else if (termStr.contains("noj")) { //noj 具体家电名称 //这里需要把noj和ntc放在n前面识别
				abstractQuery += "noj ";
				abstractMap.put("noj", word);
			} else if (termStr.contains("n")) { //n 家电类型名称
				abstractQuery += "n ";
				abstractMap.put("n", word);
			} else if (termStr.contains("dp")) { //dp 店铺名称
				abstractQuery += "dp ";
				abstractMap.put("dp", word);
			} else if (termStr.contains("att")) { //att 参数
				abstractQuery += "att ";
				abstractMap.put("att", word);
			} else if (termStr.contains("cj")) { //cj 厂家
				abstractQuery += "cj ";
				abstractMap.put("cj", word);
			} else if (termStr.contains("vsc")) { //vsc 动词
				abstractQuery += "vsc ";
				abstractMap.put("vsc", word);
			} else if (termStr.contains("mq")) { //mq 某参数
				abstractQuery += "mq ";
				abstractMap.put("mq", word);
			} else if (termStr.contains("m") && mCount == 0) { //m 数词 m1
				abstractQuery += "m1 ";
				abstractMap.put("m1", word);
				mCount++;
			} else if (termStr.contains("m") && mCount == 1) { //m 数词 第二个 m2
				abstractQuery += "m2 ";
				abstractMap.put("m2", word);
				mCount++;
			} else {
				abstractQuery += word + " ";
			}
		}

		System.out.println("========HanLP分词结束========");
		System.out.println(" ");
		process.append("========HanLP分词结束========" + "<br><br>");

		System.out.println("关键字与其词性的map键值对哈希表 abstractMap 已生成");
		System.out.println(" ");
		//process.append("关键字与其词性的map键值对哈希表已生成" + "<br><br>");

		System.out.println("模板所需的词性替换、句子抽象化处理已完成");
		System.out.println(" ");
		//process.append("模板所需的词性替换、句子抽象化处理已完成" + "<br><br>");

		return abstractQuery;
	}

	/**
	 * 句子还原
	 * 从抽象map中获取词性的集合
	 * @param queryPattern ntc n 有哪些
	 * @return 返回对应模板替换结果 海尔 冰箱 有哪些
	 */
	public  String queryExtenstion(String queryPattern) {

		System.out.println("========开始句子还原========");
		System.out.println("从abstractMap中获取词性集合");
		System.out.println("替换句子模板中的抽象词性为 具体问句中的单词");

		Set<String> set = abstractMap.keySet(); //提取key，value的keys //也就是词性比如ntc
		for (String key : set) {
			/**
			 * 如果句子模板中含有抽象的词性
			 */
			if (queryPattern.contains(key)) {

				/**
				 * 则替换抽象词性为具体的值 
				 */
				String value = abstractMap.get(key);
				queryPattern = queryPattern.replace(key, value);
			}
		}
		String extendedQuery = queryPattern;
		/**
		 * 当前句子处理完，抽象map清空释放空间并置空，等待下一个句子的处理
		 */
		abstractMap.clear();
		abstractMap = null;

		System.out.println("========句子还原结束========");
		System.out.println(" ");

//		process.append("========开始句子还原========" + "<br>"
//				+ "从abstractMap中获取词性集合" + "<br>"
//				+ "替换句子模板中的抽象词性为 具体问句中的单词" + "<br>"
//				+ "========句子还原结束========"  + "<br><br>");

		return extendedQuery;
	}

	/**
	 * 加载词汇表
	 * 单词，序号 哈希表
	 * 关键特征
	 * 与HanLP分词后的单词进行匹配
	 * @return
	 */
	public  Map<String, Integer> loadVocabulary() {

		System.out.println("========开始加载词汇表文件========");
		System.out.println("词汇表位置：" + rootDirPath + "question/vocabulary.txt");

		Map<String, Integer> vocabulary = new HashMap<String, Integer>();
		File file = new File(rootDirPath + "question/vocabulary.txt");

		System.out.println("========词汇表文件加载完毕========");
		System.out.println(" ");

		return getStringIntegerMap(vocabulary, file);
	}
	/**
	 * 由loadVocabulary()调用
	 * 构造单词与序号的对应
	 * 便于构造向量、相似匹配
	 * @param vocabulary
	 * @param file
	 * @return 返回构造好的哈希表
	 */
	private Map<String, Integer> getStringIntegerMap(Map<String, Integer> vocabulary, File file) {

		System.out.println("========开始构造 单词-序号 的哈希表========");
		System.out.println("以 : 分割读取的文件字符数据流");

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		String line;
		try {
			while ((line = br.readLine()) != null) {
				String[] tokens = line.split(":");
				// 可以直接notepad++解决
				int index = Integer.parseInt(tokens[0].replace("\uFEFF","").toString());
				String word = tokens[1];
				vocabulary.put(word, index);
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("========单词-序号 的哈希表 vocabulary 构造完毕========");
		System.out.println(" ");

		return vocabulary;
	}

	/**
	 * 加载问题模板文件
	 * 需要特殊处理，故特别定义loadFile()方法
	 * 加载文件，并读取内容返回
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public  String loadFile(String filename) throws IOException {
		File file = new File(rootDirPath + filename);
		BufferedReader br = new BufferedReader(new FileReader(file));
		String content = "";
		String line;
		while ((line = br.readLine()) != null) {
			/**
			 * 文本的换行符暂定用"`"代替
			 * 用来识别，按行读入并处理
			 */
			content += line + "`";
		}
		/**
		 * 关闭资源
		 */
		br.close();
		return content;
	}

	/**
	 * 句子分词后，或者读入问句训练集时
	 * 与词汇表进行key匹配
	 * 转换为double向量数组
	 * 供训练与预测使用
	 * @param sentence
	 * @return
	 * @throws Exception
	 */
	public  double[] sentenceToArrays(String sentence) throws Exception {

		//System.out.println("========开始生成对应词向量========");

		/**
		 * 模板对照加载的词汇表的大小进行初始化，全部为0.0
		 */
		double[] vector = new double[vocabulary.size()];
		for (int i = 0; i < vocabulary.size(); i++) {
			vector[i] = 0;
		}

		/**
		 * HanLP分词，拿分词的结果和词汇表里面的关键特征进行匹配
		 */
		Segment segment = HanLP.newSegment();
		List<Term> terms = segment.seg(sentence);
		for (Term term : terms) {
			String word = term.word;
			/**
			 * 如果命中，0.0 改为 1.0
			 */
			if (vocabulary.containsKey(word)) {
				int index = vocabulary.get(word);
				vector[index] = 1;
			}
		}

		//System.out.println("========词向量生成完毕========");
		return vector;
	}

	/**
	 * Spark朴素贝叶斯(naiveBayes)
	 * 对特定的模板进行加载
	 * 生成向量并进行训练
	 * @return 返回训练好的朴素贝叶斯分类器
	 * @throws Exception
	 */
	public  NaiveBayesModel loadClassifierModel() throws Exception {

		/**
		 * 生成Spark对象
		 *
		 * 一、Spark程序是通过SparkContext发布到Spark集群的
		 * Spark程序的运行都是在SparkContext为核心的调度器的指挥下进行的
		 * Spark程序的结束是以SparkContext结束作为结束
		 * JavaSparkContext对象用来创建Spark的核心RDD的
		 * 注意：第一个RDD,一定是由SparkContext来创建的
		 *
		 * 二、SparkContext的主构造器参数为 SparkConf
		 * SparkConf必须设置appname和master，否则会报错
		 * spark.master  用于设置部署模式
		 * local[*] 本地运行模式[也可以是集群的形式]，如果需要多个线程执行，可以设置为local[2],表示2个线程 ，*表示多个
		 * spark.app.name 用于指定应用的程序名称
		 */
		SparkConf conf = new SparkConf().setAppName("NaiveBayesTest").setMaster("local[*]").
				set("spark.driver.allowMultipleContexts", "true");
		// allowMultipleContexts选项，使Spark上下文环境可有多个
		JavaSparkContext sc = new JavaSparkContext(conf);

		/**
		 * 训练集生成
		 * labeled point 是一个局部向量，要么是密集型的要么是稀疏型的
		 * 用一个label/response进行关联。在MLlib里，labeled points 被用来监督学习算法
		 * 我们使用一个double数来存储一个label，因此我们能够使用labeled points进行回归和分类
		 */
		List<LabeledPoint> train_list = new LinkedList<LabeledPoint>();
		String[] sentences = null;


		System.out.println("========开始加载问题训练集========");


		/**
		 * 0 海尔的冰箱有哪些
		 */
		System.out.println("加载训练集0：某品牌的某类型家电");
		String pinpaiJiadian = loadFile("question/【0】某品牌的某类型家电.txt");
		sentences = pinpaiJiadian.split("`"); //根据自定义换行标记分割
		System.out.println("调试输出——问题模板0对应的向量：");
		for (String sentence : sentences) {
			double[] array = sentenceToArrays(sentence);
			System.out.println(Arrays.toString(array)); //输出向量，调试
			LabeledPoint train_one = new LabeledPoint(0.0, Vectors.dense(array));
			train_list.add(train_one);
		}

		/**
		 * 1 使用风冷制冷的冰箱有哪些
		 */
		System.out.println("加载训练集1：某种参数的某类型家电");
		String attJiadian = loadFile("question/【1】某种参数的某类型家电.txt");
		sentences = attJiadian.split("`"); //根据自定义换行标记分割
		System.out.println("调试输出——问题模板1对应的向量：");
		for (String sentence : sentences) {
			double[] array = sentenceToArrays(sentence);
			System.out.println(Arrays.toString(array));
			LabeledPoint train_one = new LabeledPoint(1.0, Vectors.dense(array)); //index
			train_list.add(train_one);
		}

		/**
		 * 2 有哪些厂家生产冰箱
		 */
		System.out.println("加载训练集2：有哪些厂商生产某类型家电");
		String jiadianPinpai = loadFile("question/【2】有哪些厂商生产某类型家电.txt");
		sentences = jiadianPinpai.split("`"); //根据自定义换行标记分割
		System.out.println("调试输出——问题模板2对应的向量：");
		for (String sentence : sentences) {
			double[] array = sentenceToArrays(sentence);
			System.out.println(Arrays.toString(array));
			LabeledPoint train_one = new LabeledPoint(2.0, Vectors.dense(array)); //index
			train_list.add(train_one);
		}

		/**
		 * 3 某冰箱的价格
		 */
		System.out.println("加载训练集3：某个家电的价格");
		String priceJiadian = loadFile("question/【3】某个家电的价格.txt");
		sentences = priceJiadian.split("`"); //根据自定义换行标记分割
		System.out.println("调试输出——问题模板3对应的向量：");
		for (String sentence : sentences) {
			double[] array = sentenceToArrays(sentence);
			System.out.println(Arrays.toString(array));
			LabeledPoint train_one = new LabeledPoint(3.0, Vectors.dense(array)); //index
			train_list.add(train_one);
		}

		/**
		 * 4 某品牌生产哪种家电
		 */
		System.out.println("加载训练集4：某品牌有哪些家电种类");
		String nNtc = loadFile("question/【4】某品牌生产哪些种类的家电.txt");
		sentences = nNtc.split("`"); //根据自定义换行标记分割
		System.out.println("调试输出——问题模板4对应的向量：");
		for (String sentence : sentences) {
			double[] array = sentenceToArrays(sentence);
			System.out.println(Arrays.toString(array));
			LabeledPoint train_one = new LabeledPoint(4.0, Vectors.dense(array)); //index
			train_list.add(train_one);
		}

		/**
		 * 5 某冰箱的所有参数
		 */
		System.out.println("加载训练集5：某个家电的所有参数");
		String allNoj = loadFile("question/【5】某个家电的所有参数.txt");
		sentences = allNoj.split("`"); //根据自定义换行标记分割
		System.out.println("调试输出——问题模板5对应的向量：");
		for (String sentence : sentences) {
			double[] array = sentenceToArrays(sentence);
			System.out.println(Arrays.toString(array));
			LabeledPoint train_one = new LabeledPoint(5.0, Vectors.dense(array)); //index
			train_list.add(train_one);
		}

		/**
		 * 6 某冰箱的某个参数
		 */
		System.out.println("加载训练集6：某个家电的某个参数");
		String mqNoj = loadFile("question/【6】某个家电的某个参数.txt");
		sentences = mqNoj.split("`"); //根据自定义换行标记分割
		System.out.println("调试输出——问题模板6对应的向量：");
		for (String sentence : sentences) {
			double[] array = sentenceToArrays(sentence);
			System.out.println(Arrays.toString(array));
			LabeledPoint train_one = new LabeledPoint(6.0, Vectors.dense(array)); //index
			train_list.add(train_one);
		}

		/**
		 * 7 价格大于1000的冰箱
		 */
		System.out.println("加载训练集7：某类型家电价格【小于】");
		String nLP = loadFile("question/【7】某类型家电价格【小于】.txt");
		sentences = nLP.split("`"); //根据自定义换行标记分割
		System.out.println("调试输出——问题模板7对应的向量：");
		for (String sentence : sentences) {
			double[] array = sentenceToArrays(sentence);
			System.out.println(Arrays.toString(array));
			LabeledPoint train_one = new LabeledPoint(7.0, Vectors.dense(array)); //index
			train_list.add(train_one);
		}

		/**
		 * 8 价格小于1000的冰箱
		 */
		System.out.println("加载训练集8：某类型家电价格【大于】");
		String nHP = loadFile("question/【8】某类型家电价格【大于】.txt");
		sentences = nHP.split("`"); //根据自定义换行标记分割
		System.out.println("调试输出——问题模板8对应的向量：");
		for (String sentence : sentences) {
			double[] array = sentenceToArrays(sentence);
			System.out.println(Arrays.toString(array));
			LabeledPoint train_one = new LabeledPoint(8.0, Vectors.dense(array)); //index
			train_list.add(train_one);
		}

		/**
		 * 9 价格在1000到2000的冰箱
		 */
		System.out.println("加载训练集9：某类型家电价格【某区间】");
		String nLH = loadFile("question/【9】某类型家电价格【某区间】.txt");
		sentences = nLH.split("`"); //根据自定义换行标记分割
		System.out.println("调试输出——问题模板9对应的向量：");
		for (String sentence : sentences) {
			double[] array = sentenceToArrays(sentence);
			System.out.println(Arrays.toString(array));
			LabeledPoint train_one = new LabeledPoint(9.0, Vectors.dense(array)); //index
			train_list.add(train_one);
		}


		System.out.println("========问题训练集加载完毕========");
		System.out.println(" ");


		/**
		 * SPARK的核心是RDD(弹性分布式数据集)
		 * Spark是Scala写的,JavaRDD就是Spark为Java写的一套API
		 * JavaSparkContext sc = new JavaSparkContext(sparkConf);    //对应JavaRDD
		 * SparkContext	    sc = new SparkContext(sparkConf)    ;    //对应RDD
		 */
		System.out.println("========开始朴素贝叶斯分类训练========");
		System.out.println("转换训练集为RDD——弹性分布式数据集");
		System.out.println("Spark-NaiveBayes生成训练器，开始训练");
		JavaRDD<LabeledPoint> trainingRDD = sc.parallelize(train_list);
		NaiveBayesModel nb_model = NaiveBayes.train(trainingRDD.rdd());
		System.out.println("========朴素贝叶斯分类训练结束========");

		/**
		 * 记得关闭资源
		 */
		sc.close();

		/**
		 * 返回贝叶斯分类器
		 */
		return nb_model;
	}

	/**
	 * 加载问题模板
	 * 分类器标签
	 * @return
	 */
	public  Map<Double, String> loadQuestionsPattern() {

		System.out.println(" ");
		System.out.println("========开始加载问题模板========");
		System.out.println("模板文件：" + rootDirPath + "question/question_classification.txt");

		System.out.println("按行读入文件，以 : 分割");
		System.out.println("构造 模板-序号 的哈希表 questionsPattern");
		Map<Double, String> questionsPattern = new HashMap<Double, String>();
		File file = new File(rootDirPath + "question/question_classification.txt");
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		String line;
		try {
			while ((line = br.readLine()) != null) { //按行读入
				String[] tokens = line.split(":"); //以:分割序号和模板 存在list中
				double index = Double.valueOf(tokens[0]);
				String pattern = tokens[1];
				questionsPattern.put(index, pattern);
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("========问题模板加载完毕========");
		System.out.println(" ");

		return questionsPattern;
	}

	/**
	 * 使用贝叶斯分类器进行预测
	 * 得到问句分类的结果（拿到匹配的分类标签号，也就是问题模板标号）
	 * 并根据标签号，在哈希表中查询
	 * 返回问题的模板
	 * @param sentence ntc 的 n 有 哪些
	 * @return 返回模板 ntc n 有哪些
	 * @throws Exception
	 */
	public  String queryClassify(String sentence) throws Exception {

		/**
		 * 问句生成对应向量数组 testArray
		 */
		System.out.println("用户问句——开始生成向量");
		double[] testArray = sentenceToArrays(sentence);
		System.out.println("用户问句——词向量生成完毕");
		System.out.println("用户问句——调试输出——用户问句的向量：" + Arrays.toString(testArray));
//		process.append("用户问句——开始生成向量" + "<br>"
//						+ "用户问句——词向量生成完毕" + "<br>"
//						+ "用户问句——调试输出——用户问句的向量：" + Arrays.toString(testArray) + "<br>");

		/**
		 * 生成对应的稠密向量 v
		 */
		Vector v = Vectors.dense(testArray);
		System.out.println("用户问句——生成的对应的稠密向量：" + v.toString());
		//process.append("用户问句——生成的对应的稠密向量：" + v.toString() + "<br>");

		/**
		 * 对数据进行预测predict
		 * 得到句子模板在 spark贝叶斯分类器中的索引【位置】
		 * 根据词汇使用的频率推断出句子对应哪一个模板
		 */
		System.out.println("用户问句——贝叶斯分类器进行预测");
		process.append("用户问句——贝叶斯分类器进行预测" + "<br>");

		double index = nbModel.predict(v);
		modelIndex = (int)index;
		//System.out.println("预测匹配完成，模板序号为" + index);
		history2 = questionsPattern.get(index);



		/**
		 * 问答历史记录
		 * 将queryString存至txt文件新的一行
		 * 原句子 模板序号 模板句式
		 */
		if(history1!="预加载" && history1!=""){
			try {
				File file = new File("history.txt");
				if(!file.exists()) {
					file.createNewFile(); // 创建新文件,有同名的文件的话直接覆盖
				}
				FileOutputStream fos = new FileOutputStream(file,true);
				OutputStreamWriter osw = new OutputStreamWriter(fos);
				BufferedWriter bw = new BufferedWriter(osw);

				bw.newLine(); //换行
				bw.write(history1 + ","); //存储原句子
				bw.write("" + index + ","); //存储模板序号
				bw.write(history2 + ""); //存储模板句式

				bw.flush();
				bw.close();
				osw.close();
				fos.close();
			}catch (FileNotFoundException e1) {
				e1.printStackTrace();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		}



		/**
		 * 问题模板匹配概率输出
		 * 训练集调试用
		 * 测试模板匹配准确率
		 */
		Vector vRes = nbModel.predictProbabilities(v);

		System.out.println("问题模板分类为【0】的概率："+vRes.toArray()[0]);
		process.append("问题模板分类为【0】的概率："+vRes.toArray()[0] + "<br>");
		System.out.println("问题模板分类为【1】的概率："+vRes.toArray()[1]);
		process.append("问题模板分类为【1】的概率："+vRes.toArray()[1] + "<br>");
		System.out.println("问题模板分类为【2】的概率："+vRes.toArray()[2]);
		process.append("问题模板分类为【2】的概率："+vRes.toArray()[2] + "<br>");
		System.out.println("问题模板分类为【3】的概率："+vRes.toArray()[3]);
		process.append("问题模板分类为【3】的概率："+vRes.toArray()[3] + "<br>");
		System.out.println("问题模板分类为【4】的概率："+vRes.toArray()[4]);
		process.append("问题模板分类为【4】的概率："+vRes.toArray()[4] + "<br>");
		System.out.println("问题模板分类为【5】的概率："+vRes.toArray()[5]);
		process.append("问题模板分类为【5】的概率："+vRes.toArray()[5] + "<br>");
		System.out.println("问题模板分类为【6】的概率："+vRes.toArray()[6]);
		process.append("问题模板分类为【6】的概率："+vRes.toArray()[6] + "<br>");

		System.out.println("朴素贝叶斯预测匹配完成，模板序号为 " + index);
		process.append("朴素贝叶斯预测匹配完成，模板序号为 " + index + "<br>");

		return questionsPattern.get(index);
	}
}
