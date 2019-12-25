package cn.kaoqin.vo;

import java.math.BigDecimal;

import lombok.Data;

//月度员工考勤情况输出表
@Data
public class KaoQinGetOutVo {
    private String name;
    private BigDecimal remainingAnnualLeaveOfLastMonth;//上月剩余年假
    private BigDecimal restOfLastMonth;//上月剩余调休
    private BigDecimal overtimeThisMonth;//本月加班
    private BigDecimal leaveDaysOfThisMonth;//本月请假天数
    private BigDecimal annualLeaveDaysOfThisMonth;//本月休年假天数
    private BigDecimal lateTimesOfThisMonth;//本月迟到次数
    private BigDecimal earlyLeaveTimesOfThisMonth;//本月早退次数
    private BigDecimal numberOfAbsenteeismThisMonth;//本月旷工次数
    private BigDecimal deductionDays;//违纪扣减天数
    private BigDecimal daysOfBusinessTrip;//出差天数
    private BigDecimal dinnerAllowanceDays;//晚餐补助天数
    private BigDecimal finalDeductionOfAttendance;//最终扣减考勤
    private BigDecimal remainingAnnualLeaveOfThisMonth;//本月剩余年假
    private BigDecimal restOfThisMonth;//本月剩余调休
}
