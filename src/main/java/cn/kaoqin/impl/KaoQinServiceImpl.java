package cn.kaoqin.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.http.HttpServletResponse;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Service;

import cn.kaoqin.service.KaoQinService;
import cn.kaoqin.vo.KaoQinExcelMainVo;
import cn.kaoqin.vo.KaoQinGetOutVo;
import cn.kaoqin.vo.LastMonthHolidaysVo;
import cn.kaoqin.vo.RecordEverydayVo;
import cn.kaoqin.vo.ThisMonthOvertimeVo;
import cn.util.BigDecimalUtils;
import cn.util.FileUtils;
import cn.util.RegTest;

/**
 * 考勤文件处理服务接口实现类
 * @author ZhaoZhigang
 *
 */
@Service
public class KaoQinServiceImpl implements KaoQinService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    //配置文件中获取考勤文件读取路径
    @Value("${params.kaoqin.name}")
    private String name;
    
    //配置文件中获取旷工迟到起始时间
    @Value("${params.kaoqin.beLateLimitMinute}")
    private int beLateLimitMinute;
    
    //配置文件中获取标准上班时间
    @Value("${params.kaoqin.standardClockIn}")
    private String standardClockIn;
    
    //配置文件中获取标准下班时间
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @Value("${params.kaoqin.standardClockOut}")
    private String standardClockOut;
    
    //配置文件中获取晚餐补助开始时间
    @Value("${params.kaoqin.subsidyStartTime}")
    private String subsidyStartTime;
    
    //配置文件中获取加班累计开始时间
    @Value("${params.kaoqin.overtimeStartTime}")
    private String overtimeStartTime;
    
    //从配置文件中读取特殊工作日配置
    @Value("${params.kaoqin.specialworkday}")
    private String specialworkday;
    
    //从配置文件中读取允许迟到的次数
    @Value("${params.kaoqin.freeBeLateTimes}")
    private int freeBeLateTimes;
    
    //从配置文件中读取一般迟到次数的上限
    @Value("${params.kaoqin.smallBeLateTimes}")
    private int smallBeLateTimes;
    
    //从配置文件中读取一般迟到扣考勤天数
    @Value("${params.kaoqin.freeBlateDays}")
    private double freeBlateDays;
    
    //从配置文件中读取旷工扣考勤天数
    @Value("${params.kaoqin.smallBlateDays}")
    private double smallBlateDays;
    
    //从配置文件中读取加班折算调休时长比例
    @Value("${params.kaoqin.overtimeRate}")
    private double overtimeRate;
    
    /**
     * 设置表头
     * @param workbook
     * @param sheet
     */
    private void setTitle(XSSFWorkbook workbook, XSSFSheet sheet){
        XSSFRow row = sheet.createRow(0);
        //设置列宽，setColumnWidth的第二个参数要乘以256，这个参数的单位是1/256个字符宽度
        int index = 0;
        sheet.setColumnWidth(index++, (int)(6.64*256));
        sheet.setColumnWidth(index++, (int)(12.91*256));
        sheet.setColumnWidth(index++, (int)(16.45*256));
        sheet.setColumnWidth(index++, (int)(15.73*256));
        sheet.setColumnWidth(index++, (int)(12.91*256));
        sheet.setColumnWidth(index++, (int)(15.09*256));
        sheet.setColumnWidth(index++, (int)(12.91*256));
        sheet.setColumnWidth(index++, (int)(12.91*256));
        sheet.setColumnWidth(index++, (int)(12.91*256));
        sheet.setColumnWidth(index++, (int)(12.91*256));
        sheet.setColumnWidth(index++, (int)(8.64*256));
        sheet.setColumnWidth(index++, (int)(12.91*256));
        sheet.setColumnWidth(index++, (int)(12.91*256));
        sheet.setColumnWidth(index++, (int)(12.91*256));
        sheet.setColumnWidth(index++, (int)(12.91*256));
        
        //设置为居中加粗，红色
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setColor(XSSFFont.COLOR_RED);
        style.setFont(font);
        
        XSSFCell cell;
        index = 0;
        cell = row.createCell(index++);
        cell.setCellValue("姓名");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("上月剩余年假");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("上月剩余调休(天)");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("本月加班(小时）");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("本月请假天数");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("本月休年假天数");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("本月迟到次数");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("本月早退次数");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("本月旷工次数");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("违纪扣减天数");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("出差天数");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("晚餐补助天数");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("最终扣减考勤");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("本月剩余年假");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("本月剩余调休");
        cell.setCellStyle(style);
    }

    @Override
    public KaoQinExcelMainVo importExcel(String filePath) {
        KaoQinExcelMainVo kaoQinExcelMainVo = new KaoQinExcelMainVo();
        try {
            //使用非递归方式获取日报存储目录下的所有文件
            ArrayList<File> fileList = FileUtils.getFiles(filePath,false);
            //遍历所有文件
            for (Iterator<File> iterator = fileList.iterator(); 
                    iterator.hasNext();) {
                File file = iterator.next();
                //如果文件不是xlsx格式的考勤文件，则扫描下一个文件
                if(!RegTest.match(file.getName(), "^中科软科技股份有限公司_考勤报表.*\\.xlsx$")) {
                    continue;
                }
                
                logger.info("正在处理文件：" + file.getName());
                FileInputStream input=null;
                XSSFWorkbook wb = null;
                try {
                    input=new FileInputStream(file);
                    // 创建文档
                    wb =new XSSFWorkbook(input);
                    //读取sheet(页)
                    kaoQinExcelMainVo.setRecordEverydayVos(this.getRecordEveryday(wb));
                    kaoQinExcelMainVo.setLastMonthHolidaysVos(this.getLastMonthHolidays(wb));
                    kaoQinExcelMainVo.setThisMonthOvertimeVos(this.getThisMonthOvertime(wb));
                } catch (IOException e) {
                    logger.error(e.getMessage(),e);
                } finally{
                    try {
                        input.close();
                    } catch (IOException e) {
                        logger.error(e.getMessage(),e);
                    }
                }
            }
        } catch (Exception e1) {
            logger.error(e1.getMessage(),e1);
        }
        return kaoQinExcelMainVo;
    }
    
    private ArrayList<ThisMonthOvertimeVo> getThisMonthOvertime(XSSFWorkbook wb) {
        XSSFSheet xssfSheet = wb.getSheet("本月加班");
        ArrayList<ThisMonthOvertimeVo> thisMonthOvertimeVos = new ArrayList<>();
        for (int i = 1; i <= xssfSheet.getLastRowNum(); i++) {
            XSSFRow xssfRow = xssfSheet.getRow(i);
            ThisMonthOvertimeVo thisMonthOvertimeVo = new ThisMonthOvertimeVo();
            thisMonthOvertimeVo.setName(xssfRow.getCell(0).toString());
            thisMonthOvertimeVo.setOvertimeHours(xssfRow.getCell(1).toString());
            thisMonthOvertimeVos.add(thisMonthOvertimeVo);
        }
        return thisMonthOvertimeVos;
    }

    private ArrayList<LastMonthHolidaysVo> getLastMonthHolidays(XSSFWorkbook wb) {
        XSSFSheet xssfSheet = wb.getSheet("上月剩余假期");
        ArrayList<LastMonthHolidaysVo> lastMonthHolidaysVos = new ArrayList<>();
        for (int i = 1; i <= xssfSheet.getLastRowNum(); i++) {
            XSSFRow xssfRow = xssfSheet.getRow(i);
            LastMonthHolidaysVo LastMonthHolidaysVo = new LastMonthHolidaysVo();
            LastMonthHolidaysVo.setName(xssfRow.getCell(0).toString());
            LastMonthHolidaysVo.setAnnualLeave(xssfRow.getCell(1).toString());
            LastMonthHolidaysVo.setOvertimeHours(xssfRow.getCell(2).toString());
            lastMonthHolidaysVos.add(LastMonthHolidaysVo);
        }
        return lastMonthHolidaysVos;
    }

    private ArrayList<RecordEverydayVo> getRecordEveryday(XSSFWorkbook wb) {
        XSSFSheet xssfSheet = wb.getSheet("每日统计");
        ArrayList<RecordEverydayVo> recordEverydayVos = new ArrayList<>();
        for (int i = 4; i <= xssfSheet.getLastRowNum(); i++) {
            XSSFRow xssfRow = xssfSheet.getRow(i);
            RecordEverydayVo recordEverydayVo = new RecordEverydayVo();
            recordEverydayVo.setName(xssfRow.getCell(0).toString());
            recordEverydayVo.setDept(xssfRow.getCell(1).toString());
            recordEverydayVo.setId(xssfRow.getCell(2).toString());
            recordEverydayVo.setPosition(xssfRow.getCell(3).toString());
            recordEverydayVo.setDate(xssfRow.getCell(5).toString());
            recordEverydayVo.setJobTime(xssfRow.getCell(6).toString());
            recordEverydayVo.setAmClockIn(xssfRow.getCell(7).toString());
            recordEverydayVo.setAmClockInResult(xssfRow.getCell(8).toString());
            recordEverydayVo.setPmClockOut(xssfRow.getCell(9).toString());
            recordEverydayVo.setPmClockOutResult(xssfRow.getCell(10).toString());
            recordEverydayVo.setAssociatedApproval(xssfRow.getCell(15).toString());
            recordEverydayVos.add(recordEverydayVo);
        }
        return recordEverydayVos;
    }

    @Override
    public void exportExcel(HashMap<String, KaoQinGetOutVo> kaoQinGetOutMap, HttpServletResponse response,
            String saveType, String filePath) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        //创建sheet页
        XSSFSheet sheet = workbook.createSheet("考勤统计结果");
        SimpleDateFormat sdfFilename = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");

        //创建支出页表头
        this.setTitle(workbook, sheet);
        int rowNum = 1;
        String [] splitname=name.split(";");
        for (String personName : splitname) { 
            KaoQinGetOutVo kaoQinGetOutVo = kaoQinGetOutMap.get(personName);
            XSSFRow row = sheet.createRow(rowNum);
            int index = 0;
            row.createCell(index++).setCellValue(kaoQinGetOutVo.getName());// 姓名
            row.createCell(index++).setCellValue(kaoQinGetOutVo.getRemainingAnnualLeaveOfLastMonth().doubleValue());// 上月剩余年假
            row.createCell(index++).setCellValue(kaoQinGetOutVo.getRestOfLastMonth().doubleValue());// 上月剩余调休
            row.createCell(index++).setCellValue(kaoQinGetOutVo.getOvertimeThisMonth().doubleValue());// 本月加班
            row.createCell(index++).setCellValue(kaoQinGetOutVo.getLeaveDaysOfThisMonth().doubleValue());// 本月请假天数
            row.createCell(index++).setCellValue(kaoQinGetOutVo.getAnnualLeaveDaysOfThisMonth().doubleValue());// 本月休年假天数
            row.createCell(index++).setCellValue(kaoQinGetOutVo.getLateTimesOfThisMonth().doubleValue());// 本月迟到次数
            row.createCell(index++).setCellValue(kaoQinGetOutVo.getEarlyLeaveTimesOfThisMonth().doubleValue());// 本月早退次数
            row.createCell(index++).setCellValue(kaoQinGetOutVo.getNumberOfAbsenteeismThisMonth().doubleValue());// 本月旷工次数
            row.createCell(index++).setCellValue(kaoQinGetOutVo.getDeductionDays().doubleValue());// 违纪扣减天数
            row.createCell(index++).setCellValue(kaoQinGetOutVo.getDaysOfBusinessTrip().doubleValue());// 出差天数
            row.createCell(index++).setCellValue(kaoQinGetOutVo.getDinnerAllowanceDays().doubleValue());// 晚餐补助天数
            row.createCell(index++).setCellValue(kaoQinGetOutVo.getFinalDeductionOfAttendance().doubleValue());// 最终扣减考勤
            row.createCell(index++).setCellValue(kaoQinGetOutVo.getRemainingAnnualLeaveOfThisMonth().doubleValue());// 本月剩余年假
            row.createCell(index++).setCellValue(kaoQinGetOutVo.getRestOfThisMonth().doubleValue());// 本月剩余调休
            
            rowNum++;
        }

        //以当前时间命名导出的考勤文件
        String fileName = "考勤统计结果_"+
                sdfFilename.format(new Date())+".xlsx";
        if("download".equals(saveType)) {
            //清空response  
            response.reset();  
            //设置response的Header  
            response.addHeader("Content-Disposition", "attachment;filename="+
                    fileName);  
            OutputStream os = null;
            try {
                os = new BufferedOutputStream(response.getOutputStream());
            } catch (IOException e1) {
                logger.error(e1.getMessage(),e1);
            }  
            response.setContentType("application/vnd.ms-excel;charset=gb2312"); 
            //将excel写入到输出流中
            try {
                workbook.write(os);
            } catch (IOException e) {
                logger.error(e.getMessage(),e);
            }
            try {
                os.flush();
            } catch (IOException e) {
                logger.error(e.getMessage(),e);
            }
            try {
                os.close();
            } catch (IOException e) {
                logger.error(e.getMessage(),e);
            }
        }else if("save".equals(saveType)) {
            FileOutputStream output = null;
            try {
                //判断是否存在目录. 不存在则创建
                FileUtils.createNewFile(filePath+"\\"+fileName);
                //输出Excel文件  
                output = new FileOutputStream(filePath+"\\"+fileName);
                workbook.write(output);//写入磁盘  
            } catch (FileNotFoundException e1) {
                logger.error(e1.getMessage(),e1);
            } catch (IOException e) {
                logger.error(e.getMessage(),e);
            }finally {
                try {
                    output.close();
                } catch (IOException e) {
                }
            }
        }
    }

    @Override
    public HashMap<String, KaoQinGetOutVo> convertObject(KaoQinExcelMainVo kaoQinExcelMainVo) {
        HashMap<String, KaoQinGetOutVo> kaoQinGetOutMap = new HashMap<>();
        String [] splitname=name.split(";");
        for (String personName : splitname) {    
            KaoQinGetOutVo kaoQinGetOutVo = new KaoQinGetOutVo();
            kaoQinGetOutVo.setName(personName);
            //设置属性remainingAnnualLeaveOfLastMonth 上月剩余年假
            kaoQinGetOutVo.setRemainingAnnualLeaveOfLastMonth(BigDecimal.valueOf(0));
            //设置属性restOfLastMonth 上月剩余调休
            kaoQinGetOutVo.setRestOfLastMonth(BigDecimal.valueOf(0));
            //设置属性overtimeThisMonth 本月加班
            kaoQinGetOutVo.setOvertimeThisMonth(BigDecimal.valueOf(0));
            //设置属性leaveDaysOfThisMonth 本月请假天数
            kaoQinGetOutVo.setLeaveDaysOfThisMonth(BigDecimal.valueOf(0));
            //设置属性annualLeaveDaysOfThisMonth 本月休年假天数
            kaoQinGetOutVo.setAnnualLeaveDaysOfThisMonth(BigDecimal.valueOf(0));
            //设置属性lateTimesOfThisMonth 本月迟到次数
            kaoQinGetOutVo.setLateTimesOfThisMonth(BigDecimal.valueOf(0));
            //设置属性earlyLeaveTimesOfThisMonth 本月早退次数
            kaoQinGetOutVo.setEarlyLeaveTimesOfThisMonth(BigDecimal.valueOf(0));
            //设置属性numberOfAbsenteeismThisMonth 本月旷工次数
            kaoQinGetOutVo.setNumberOfAbsenteeismThisMonth(BigDecimal.valueOf(0));
            //设置属性deductionDays 违纪扣减天数
            kaoQinGetOutVo.setDeductionDays(BigDecimal.valueOf(0));
            //设置属性daysOfBusinessTrip 出差天数
            kaoQinGetOutVo.setDaysOfBusinessTrip(BigDecimal.valueOf(0));
            //设置属性dinnerAllowanceDays 晚餐补助天数
            kaoQinGetOutVo.setDinnerAllowanceDays(BigDecimal.valueOf(0));
            //设置属性finalDeductionOfAttendance 最终扣减考勤
            kaoQinGetOutVo.setFinalDeductionOfAttendance(BigDecimal.valueOf(0));
            //设置属性remainingAnnualLeaveOfThisMonth 本月剩余年假
            kaoQinGetOutVo.setRemainingAnnualLeaveOfThisMonth(BigDecimal.valueOf(0));
            //设置属性restOfThisMonth 本月剩余调休
            kaoQinGetOutVo.setRestOfThisMonth(BigDecimal.valueOf(0));

            kaoQinGetOutMap.put(personName,kaoQinGetOutVo);
        }
        this.calRecordEveryDay(kaoQinGetOutMap,kaoQinExcelMainVo.getRecordEverydayVos());
        this.calOvertime(kaoQinGetOutMap,kaoQinExcelMainVo.getThisMonthOvertimeVos());
        this.calHoliday(kaoQinGetOutMap,kaoQinExcelMainVo.getLastMonthHolidaysVos());
        this.calResult(kaoQinGetOutMap);
        return kaoQinGetOutMap;
    }

    private void calResult(HashMap<String, KaoQinGetOutVo> kaoQinGetOutMap) {
        for(String personName : kaoQinGetOutMap.keySet()) {
            KaoQinGetOutVo kaoQinGetOutVo = kaoQinGetOutMap.get(personName);
            
            if(kaoQinGetOutVo.getName().equals("赵志刚")) {
                System.out.println("开始调试");
            }
            
            int lateTimes = kaoQinGetOutVo.getLateTimesOfThisMonth().add(kaoQinGetOutVo.getEarlyLeaveTimesOfThisMonth()).intValue();
            double deductionDays = 0;
            if(lateTimes > this.freeBeLateTimes && lateTimes <= this.smallBeLateTimes) {
                deductionDays=BigDecimalUtils.add(deductionDays, 
                        BigDecimalUtils.mul(lateTimes-this.freeBeLateTimes, this.freeBlateDays));
            }else if(lateTimes > this.smallBeLateTimes){
                deductionDays=BigDecimalUtils.add(BigDecimalUtils.
                            add(deductionDays, 
                                    BigDecimalUtils.mul(this.smallBeLateTimes-this.freeBeLateTimes, this.freeBlateDays)),
                        BigDecimalUtils.mul(lateTimes-this.smallBeLateTimes,this.smallBlateDays));
            }
            deductionDays = BigDecimalUtils.add(deductionDays, kaoQinGetOutVo.getNumberOfAbsenteeismThisMonth().
                    multiply(BigDecimal.valueOf(this.smallBlateDays)).doubleValue());
            kaoQinGetOutVo.setDeductionDays(BigDecimal.valueOf(deductionDays));
            
            double overtimeThisMonth = kaoQinGetOutVo.getOvertimeThisMonth().doubleValue();
            overtimeThisMonth = BigDecimalUtils.div(overtimeThisMonth, 8/this.overtimeRate);
            double restOfLastMonth = kaoQinGetOutVo.getRestOfLastMonth().doubleValue();
            double leaveDaysOfThisMonth = kaoQinGetOutVo.getLeaveDaysOfThisMonth().doubleValue();
            double finalDeductionOfAttendance = 0;
            double restOfThisMonth = 0;
            restOfThisMonth = BigDecimalUtils.sub(BigDecimalUtils.add(restOfLastMonth,overtimeThisMonth),
                    BigDecimalUtils.mul(leaveDaysOfThisMonth,0.5));
            if(restOfThisMonth>=0) {
                finalDeductionOfAttendance = 0;
            }else {
                if(restOfThisMonth>-1) {
                    finalDeductionOfAttendance = 0;
                }else {
                    finalDeductionOfAttendance = Math.floor(-restOfThisMonth);
                    restOfThisMonth = BigDecimalUtils.add(restOfThisMonth, finalDeductionOfAttendance);
                }
            }
            kaoQinGetOutVo.setRestOfThisMonth(BigDecimal.valueOf(restOfThisMonth));
            kaoQinGetOutVo.setFinalDeductionOfAttendance(BigDecimal.valueOf(finalDeductionOfAttendance).
                    add(kaoQinGetOutVo.getDeductionDays()));
            
            kaoQinGetOutVo.setRemainingAnnualLeaveOfThisMonth(kaoQinGetOutVo.getRemainingAnnualLeaveOfLastMonth().
                    subtract(kaoQinGetOutVo.getAnnualLeaveDaysOfThisMonth()));
        }
    }

    private void calHoliday(HashMap<String, KaoQinGetOutVo> kaoQinGetOutMap,
            ArrayList<LastMonthHolidaysVo> lastMonthHolidaysVos) {
        for (Iterator<LastMonthHolidaysVo> iterator = lastMonthHolidaysVos.iterator(); iterator.hasNext();) {
            LastMonthHolidaysVo lastMonthHolidaysVo = iterator.next();
            if(kaoQinGetOutMap.containsKey(lastMonthHolidaysVo.getName())) {
                KaoQinGetOutVo kaoQinGetOutVo = kaoQinGetOutMap.get(lastMonthHolidaysVo.getName());
                kaoQinGetOutVo.setRestOfLastMonth(kaoQinGetOutVo.getRestOfLastMonth().
                        add(new BigDecimal(lastMonthHolidaysVo.getOvertimeHours())));
                kaoQinGetOutVo.setRemainingAnnualLeaveOfLastMonth(kaoQinGetOutVo.getRemainingAnnualLeaveOfLastMonth().
                        add(new BigDecimal(lastMonthHolidaysVo.getAnnualLeave())));
            }
        }
    }

    private void calOvertime(HashMap<String, KaoQinGetOutVo> kaoQinGetOutMap,
            ArrayList<ThisMonthOvertimeVo> thisMonthOvertimeVos) {
        for (Iterator<ThisMonthOvertimeVo> iterator = thisMonthOvertimeVos.iterator(); iterator.hasNext();) {
            ThisMonthOvertimeVo thisMonthOvertimeVo = iterator.next();
            if(kaoQinGetOutMap.containsKey(thisMonthOvertimeVo.getName())) {
                KaoQinGetOutVo kaoQinGetOutVo = kaoQinGetOutMap.get(thisMonthOvertimeVo.getName());
                kaoQinGetOutVo.setOvertimeThisMonth(kaoQinGetOutVo.getOvertimeThisMonth().
                        add(new BigDecimal(thisMonthOvertimeVo.getOvertimeHours())));
            }
        }
        
    }

    @SuppressWarnings("deprecation")
    private void calRecordEveryDay(HashMap<String, KaoQinGetOutVo> kaoQinGetOutMap,
            ArrayList<RecordEverydayVo> recordEverydayVos) {
        Date standardClockInDate = this.convertTime(this.standardClockIn.replace(";", ":"));
        Date standardClockOutDate = this.convertTime(this.standardClockOut.replace(";", ":"));
        Date subsidyStartTimeDate = this.convertTime(this.subsidyStartTime.replace(";", ":"));
        Date overtimeStartTimeDate = this.convertTime(this.overtimeStartTime.replace(";", ":"));
        
        for (Iterator<RecordEverydayVo> iterator = recordEverydayVos.iterator(); iterator.hasNext();) {
            RecordEverydayVo recordEverydayVo = iterator.next();
            if(kaoQinGetOutMap.containsKey(recordEverydayVo.getName())) {
                KaoQinGetOutVo kaoQinGetOutVo = kaoQinGetOutMap.get(recordEverydayVo.getName());
                Date nowDate = this.convertDate(recordEverydayVo.getDate());
                if(this.isWorkDay(nowDate)) {
                    if(recordEverydayVo.getAmClockInResult().contains("请假")) {
                        if(recordEverydayVo.getAssociatedApproval().contains("年假")) {
                            kaoQinGetOutVo.setAnnualLeaveDaysOfThisMonth(kaoQinGetOutVo.getAnnualLeaveDaysOfThisMonth().
                                    add(BigDecimal.valueOf(1)));
                        }else {
                            kaoQinGetOutVo.setLeaveDaysOfThisMonth(kaoQinGetOutVo.getLeaveDaysOfThisMonth().
                                    add(BigDecimal.valueOf(1)));
                        }
                    }
                    if(recordEverydayVo.getPmClockOutResult().contains("请假")) {
                        if(recordEverydayVo.getAssociatedApproval().contains("年假")) {
                            kaoQinGetOutVo.setAnnualLeaveDaysOfThisMonth(kaoQinGetOutVo.getAnnualLeaveDaysOfThisMonth().
                                    add(BigDecimal.valueOf(1)));
                        }else {
                            kaoQinGetOutVo.setLeaveDaysOfThisMonth(kaoQinGetOutVo.getLeaveDaysOfThisMonth().
                                    add(BigDecimal.valueOf(1)));
                        }
                    }
                    
                    if(recordEverydayVo.getPmClockOutResult().contains("出差")
                            ||recordEverydayVo.getAmClockInResult().contains("出差")) {
                        kaoQinGetOutVo.setDaysOfBusinessTrip(kaoQinGetOutVo.getDaysOfBusinessTrip().
                                add(BigDecimal.valueOf(1)));
                    }
                    
                    String amClockIn = recordEverydayVo.getAmClockIn();
                    if(recordEverydayVo.getAmClockInResult().contains("迟到")||
                            recordEverydayVo.getAmClockInResult().contains("缺卡")) {
                        if(amClockIn.contains(":")) {
                            Date dateClockIn = this.convertTime(amClockIn);
                            long diffMinute = (dateClockIn.getTime()-standardClockInDate.getTime())/1000/60;
                            if(diffMinute<beLateLimitMinute) {
                                kaoQinGetOutVo.setLateTimesOfThisMonth(kaoQinGetOutVo.getLateTimesOfThisMonth().
                                        add(BigDecimal.valueOf(1)));
                            }else {
                                kaoQinGetOutVo.setNumberOfAbsenteeismThisMonth(kaoQinGetOutVo.getNumberOfAbsenteeismThisMonth().
                                        add(BigDecimal.valueOf(1)));
                            }
                        }else {
                            kaoQinGetOutVo.setNumberOfAbsenteeismThisMonth(kaoQinGetOutVo.getNumberOfAbsenteeismThisMonth().
                                    add(BigDecimal.valueOf(1)));
                        }
                    }
                    String pmClockOut = recordEverydayVo.getPmClockOut();
                    if(recordEverydayVo.getPmClockOutResult().contains("早退")||
                            recordEverydayVo.getPmClockOutResult().contains("缺卡")) {
                        if(pmClockOut.contains(":")) {
                            Date dateClockOut = this.convertTime(pmClockOut);
                            long diffMinute = (standardClockOutDate.getTime()-dateClockOut.getTime())/1000/60;
                            //卢玥现在哺乳假，不计早退
                            if(!"卢玥".equals(recordEverydayVo.getName())){
                                if(diffMinute<beLateLimitMinute) {
                                    kaoQinGetOutVo.setEarlyLeaveTimesOfThisMonth(kaoQinGetOutVo.getEarlyLeaveTimesOfThisMonth().
                                            add(BigDecimal.valueOf(1)));
                                }else {
                                    kaoQinGetOutVo.setNumberOfAbsenteeismThisMonth(kaoQinGetOutVo.getNumberOfAbsenteeismThisMonth().
                                            add(BigDecimal.valueOf(1)));
                                }
                            }
                            
                            if(subsidyStartTimeDate.getTime()-dateClockOut.getTime()<0) {
                                kaoQinGetOutVo.setDinnerAllowanceDays(kaoQinGetOutVo.getDinnerAllowanceDays().
                                        add(BigDecimal.valueOf(1)));
                            }
                        }else {
                            kaoQinGetOutVo.setNumberOfAbsenteeismThisMonth(kaoQinGetOutVo.getNumberOfAbsenteeismThisMonth().
                                    add(BigDecimal.valueOf(1)));
                        }
                    }else if (recordEverydayVo.getPmClockOutResult().contains("正常")||
                            recordEverydayVo.getPmClockOutResult().contains("补卡")) {
                        if(pmClockOut.contains(":")) {
                            Date dateClockOut = this.convertTime(pmClockOut);
                            long diffMinute = (dateClockOut.getTime() - overtimeStartTimeDate.getTime())/1000/60;
                            if(diffMinute<0){
                                diffMinute = 0;
                            }
                            double diffHour = Math.floorDiv(diffMinute, 60);
                            kaoQinGetOutVo.setOvertimeThisMonth(kaoQinGetOutVo.getOvertimeThisMonth().
                                    add(BigDecimal.valueOf(diffHour)));
                        }
                    }
                }else {
                    String amClockIn = recordEverydayVo.getAmClockIn();
                    String amClockOut = recordEverydayVo.getPmClockOut();
                    if(amClockIn.contains(":")&&amClockOut.contains(":")) {
                        Date dateClockIn = this.convertTime(amClockIn);
                        Date dateClockOut = this.convertTime(amClockOut);
                        double diffHour = (dateClockOut.getTime()-dateClockIn.getTime())/1000/60/60-1;
                        if(diffHour<0) {
                            diffHour = 0;
                        }
                        if(nowDate.getDay()==1) {
                            diffHour = diffHour/this.overtimeRate;
                        }
                        kaoQinGetOutVo.setOvertimeThisMonth(kaoQinGetOutVo.getOvertimeThisMonth().
                                add(BigDecimal.valueOf(diffHour)));
                    }
                }
            }
        }
    }
    
    private Date convertTime(String time) {
        if(time.contains(":")) {
            Calendar calendar = Calendar.getInstance();
            calendar.clear();
            String hour = time.split(":")[0];
            String minute = time.split(":")[1];
            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour));
            calendar.set(Calendar.MINUTE, Integer.parseInt(minute));
            return calendar.getTime();
        }else {
            return null;
        }
    }
    
    private Date convertDate(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return sdf.parse("20"+date.split(" ")[0]);
        } catch (ParseException e) {
            return null;
        }
    }
    
    private boolean isWorkDay(Date checkDate) {
        boolean result = true;
        //解析配置文件中的特殊工作日配置为map,value为1为工作日，0为非工作日
        HashMap<String, String> wordDayMap = new HashMap<>();
        String workday[] = this.specialworkday.split(";");
        for (int i = 0; i < workday.length; i++) {
            String str[] = workday[i].split(":");
            wordDayMap.put(str[0], str[1]);
        }
        //定义日历，将校验日期设置进去，以方便后面操作
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(checkDate);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.clear(Calendar.MINUTE);
        calendar.clear(Calendar.SECOND);
        calendar.clear(Calendar.MILLISECOND);
        int week = calendar.get(Calendar.DAY_OF_WEEK);
        String specialResult = wordDayMap.get(new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime()));
        //先判断校验日期是否为特殊工作日,如果有,直接返回结果.
        if(specialResult!=null) {
            if("0".equals(specialResult)) {
                result = false;
            }else {
                result = true;
            }
        }
        //如果不是特殊工作日，则按星期判断
        if(week == 1 || week == 7) {
            result = false;
        }else {
            result = true;
        }
        return result;
    }
}
