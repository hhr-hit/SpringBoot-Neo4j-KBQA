import org.junit.Test;

import java.io.*;
import java.util.Arrays;

public class FileTest {

    @Test
    public void TestA() throws Exception {
        File filer = new File("history.txt"); //读文件
        if(!filer.exists()){ //读文件
            System.out.println("null"); //读文件
        } //读文件
        BufferedReader reader = null; //读文件
        int[] counts = new int[10]; //记录每个模板出现的次数
        int   max    = 0; //记录出现次数最多的序号
        String result = ""; //最终返回
        String[][] qs = new String[50][3]; //记录每行
        int k = 0; //计数
        try {
            FileInputStream fileInputStream = new FileInputStream(filer); //读文件
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8"); //读文件
            reader = new BufferedReader(inputStreamReader); //读文件
            String tempString = null; //读文件
            while ((tempString = reader.readLine()) != null){ //读文件
                String[] strArr = tempString.split(","); //以,分割每一行
                //System.out.println(strArr[0] + strArr[1] +strArr[2]);
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
//        for(int i=0; i<10; i++){
//            System.out.println(counts[i]);
//        }
//        Arrays.sort(counts); //排序
//        max = counts[counts.length-1]; //获得出现最多的序号max
//        System.out.println(max);
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
            //System.out.println("qs-i-0"+qs[i][0]);
            if(qs[i][0].equals(max+".0")){ //是出现最多的
                result = qs[i][1]; //将问句内容返回
                break;
            }
        } //获得出现最多的问句内容

        System.out.println(result);
    }

}
