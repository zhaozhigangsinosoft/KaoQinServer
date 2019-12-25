package cn.kaoqin.vo;

import lombok.Data;

/**
 * 考勤对象VO类
 * @author ZhaoZhigang
 *
 */
@Data
public class RecordEverydayVo {
    private String name;//名字
    private String dept;//部门
    private String id;//工号
    private String position;//职位
    private String date;//日期
    private String jobTime;//班次
    private String amClockIn;//上午打卡时间
    private String amClockInResult;//上午打卡时间
    private String pmClockOut;//下午打卡时间
    private String pmClockOutResult;//下午打卡时间
    private String associatedApproval;//关联审批单
    
//    private Date day;//日期
//    private String leave;//关联审批单   请假
//    private Integer leavenum;//除年 假出差 外请假
//    private Date amtime;//上午打卡时间
//    private Date pmtime;//下午打卡时间
//    private Integer annual;//年假
//    private Integer work;//正常考勤
//    private double latenum;//迟到次数
//    private double earlynum;//早退次数
//    private double absencenum;//旷工次数    
//    private Integer extra;//加班
//    private Integer mealallowance;//加班饭补次数
//    private double rest;//调休小时长
//    private double payannual;//已休年假
}
