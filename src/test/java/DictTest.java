import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.dictionary.CustomDictionary;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import java.io.*;
import java.util.List;

public class DictTest {

    //@Value("${HanLP.CustomDictionary.path.mcsDict}")
    private String mcsDictPath = "myHanLP/data/dictionary/custom/mqDict.txt";

    public void addCustomDictionary(BufferedReader br, int type) {
        String word;
        try {
            while ((word = br.readLine()) != null) {
                //System.out.println(new String(word.getBytes("utf-8")));
                switch (type) {
                    /**
                     * 设置某参数 词性 == mq 1000
                     */
                    case 0:
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

    @Test
    public void TestA(){
        String lineStr = "货号\n" +
                "编号\n" +
                "毛重\n" +
                "品牌\n" +
                "型号\n" +
                "产地\n" +
                "价格\n" +
                "产品匹数\n" +
                "产品渠道\n" +
                "产品特色\n" +
                "产品类型\n" +
                "冷暖类型\n" +
                "出水方式\n" +
                "制冷方式\n" +
                "加热功率\n" +
                "压缩机\n" +
                "变频/定频\n" +
                "外型设计\n" +
                "安装费用\n" +
                "容积\n" +
                "宽度\n" +
                "屏幕尺寸\n" +
                "店铺\n" +
                "总容积\n" +
                "排水类型\n" +
                "控制方式\n" +
                "控温方式\n" +
                "是否防冻\n" +
                "气源\n" +
                "洗涤容量\n" +
                "深度\n" +
                "温度显示\n" +
                "燃热出水量\n" +
                "电机类型\n" +
                "电视类型\n" +
                "能效等级\n" +
                "观看距离\n" +
                "进水方式\n" +
                "适用人数\n" +
                "适用户型\n" +
                "选购指数\n" +
                "门款式\n" +
                "面板材质\n" +
                "面板颜色\n" +
                "颜色\n" +
                "高度\n" +
                "京选燃气热水器\n" +
                "京选电热水器\n" +
                "特色推荐\n" +
                "用户优选";
        try{
            Segment segment = HanLP.newSegment();
            segment.enableCustomDictionary(true);

            /**
             * 自定义分词+词性
             */
            File file = new File(mcsDictPath);
            //System.out.println("到这了");
            BufferedReader br = null;

            try {
                br = new BufferedReader(new FileReader(file));
                System.out.println("文件读取成功" + br);
                addCustomDictionary(br, 0);
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            }

            List<Term> seg = segment.seg(lineStr);
            for (Term term : seg) {
                System.out.println(term.toString());
            }
        }catch(Exception ex){
            System.out.println("出错了" + ex.getClass()+","+ex.getMessage());
        }
    }
}
