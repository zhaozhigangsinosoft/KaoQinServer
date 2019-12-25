package cn.kaoqin.vo;

import java.util.ArrayList;

import lombok.Data;

@Data
public class KaoQinExcelMainVo {
    private ArrayList<RecordEverydayVo> recordEverydayVos;
    private ArrayList<LastMonthHolidaysVo> lastMonthHolidaysVos;
    private ArrayList<ThisMonthOvertimeVo> thisMonthOvertimeVos;
}
