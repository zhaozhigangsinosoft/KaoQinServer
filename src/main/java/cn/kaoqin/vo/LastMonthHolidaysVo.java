package cn.kaoqin.vo;

import lombok.Data;

@Data
public class LastMonthHolidaysVo {
    private String name;//姓名
    private String annualLeave;//剩余年假
    private String overtimeHours;//剩余调休
}
