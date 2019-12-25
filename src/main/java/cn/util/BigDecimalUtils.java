package cn.util;

import java.math.BigDecimal;

public class BigDecimalUtils {
    /**
     * BigDecimal加法
     * 
     * @param d1
     * @param d2
     * @return
     */
    public static double add(double d1, double d2) {
        BigDecimal b1 = BigDecimal.valueOf(d1);
        BigDecimal b2 = BigDecimal.valueOf(d2);
        return b1.add(b2).doubleValue();
    }

    /**
     * BigDecimal减法
     * 
     * @param d1
     * @param d2
     * @return
     */
    public static double sub(double d1, double d2) {
        BigDecimal b1 = BigDecimal.valueOf(d1);
        BigDecimal b2 = BigDecimal.valueOf(d2);
        return b1.subtract(b2).doubleValue();
    }

    /**
     * BigDecimal乘法
     * 
     * @param d1
     * @param d2
     * @return
     */
    public static double mul(double d1, double d2) {
        BigDecimal b1 = BigDecimal.valueOf(d1);
        BigDecimal b2 = BigDecimal.valueOf(d2);
        return b1.multiply(b2).doubleValue();
    }

    /**
     * BigDecimal除法
     * 
     * @param d1
     * @param d2
     * @return
     */
    public static double div(double d1, double d2) {
        BigDecimal b1 = BigDecimal.valueOf(d1);
        BigDecimal b2 = BigDecimal.valueOf(d2);
        if(b2.doubleValue()==0D){
            return 0D;
        }else{
            return b1.divide(b2, 20, BigDecimal.ROUND_HALF_UP).doubleValue();
        }
    }
}
