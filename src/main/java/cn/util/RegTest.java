package cn.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * 正则表达试验证类
 * @author ZhaoZhigang
 *
 */
public class RegTest {
    /**
     * 验证字符串是否满足正则表达式规则
     * @param value 待验证字符串
     * @param patternString 正则表达式字符串
     * @return boolean true为满足，false为不满足
     */
    public static boolean match(String value, String patternString) {
        Pattern pattern = Pattern.compile(patternString);
        Matcher result = pattern.matcher(value);
        if (!result.matches()) {
            return false;
        }
        return true;
    }
}
