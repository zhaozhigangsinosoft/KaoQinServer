package cn.kaoqin.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
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
     * 从路径下解析考勤表excel
     * @param filePath
     * @return
     */
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
                    //生成每日考勤记录对象列表，存储到主对象中
                    kaoQinExcelMainVo.setRecordEverydayVos(this.getRecordEveryday(wb));
                    //生成上月剩余假期对象列表，存储到主对象中
                    kaoQinExcelMainVo.setLastMonthHolidaysVos(this.getLastMonthHolidays(wb));
                    //生成本月加班时长对象列表，存储到主对象中
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
    
    /**
     * 生成本月加班时长对象列表，存储到主对象中
     * @param wb
     * @return
     */
    private ArrayList<ThisMonthOvertimeVo> getThisMonthOvertime(XSSFWorkbook wb) {
        XSSFSheet xssfSheet = wb.getSheet("本月加班");
        ArrayList<ThisMonthOvertimeVo> thisMonthOvertimeVos = new ArrayList<>();
        for (int i = 1; i <= xssfSheet.getLastRowNum(); i++) {
            XSSFRow xssfRow = xssfSheet.getRow(i);
            ThisMonthOvertimeVo thisMonthOvertimeVo = new ThisMonthOvertimeVo();
            thisMonthOvertimeVo.setName(xssfRow.getCell(0).toString());//姓名
            thisMonthOvertimeVo.setOvertimeHours(xssfRow.getCell(1).toString());//本月加班时间,单位小时
            thisMonthOvertimeVos.add(thisMonthOvertimeVo);
        }
        return thisMonthOvertimeVos;
    }

    /**
     * 生成上月剩余假期对象列表，存储到主对象中
     * @param wb
     * @return
     */
    private ArrayList<LastMonthHolidaysVo> getLastMonthHolidays(XSSFWorkbook wb) {
        XSSFSheet xssfSheet = wb.getSheet("上月剩余假期");
        ArrayList<LastMonthHolidaysVo> lastMonthHolidaysVos = new ArrayList<>();
        for (int i = 1; i <= xssfSheet.getLastRowNum(); i++) {
            XSSFRow xssfRow = xssfSheet.getRow(i);
            LastMonthHolidaysVo LastMonthHolidaysVo = new LastMonthHolidaysVo();
            LastMonthHolidaysVo.setName(xssfRow.getCell(0).toString());//姓名
            LastMonthHolidaysVo.setAnnualLeave(xssfRow.getCell(1).toString());//上个月剩余年假
            LastMonthHolidaysVo.setOvertimeHours(xssfRow.getCell(2).toString());//上个月剩余调休
            lastMonthHolidaysVos.add(LastMonthHolidaysVo);
        }
        return lastMonthHolidaysVos;
    }

    /**
     * 生成每日考勤记录对象列表，存储到主对象中
     * @param wb
     * @return
     */
    private ArrayList<RecordEverydayVo> getRecordEveryday(XSSFWorkbook wb) {
        XSSFSheet xssfSheet = wb.getSheet("每日统计");
        ArrayList<RecordEverydayVo> recordEverydayVos = new ArrayList<>();
        for (int i = 4; i <= xssfSheet.getLastRowNum(); i++) {
            XSSFRow xssfRow = xssfSheet.getRow(i);
            RecordEverydayVo recordEverydayVo = new RecordEverydayVo();
            recordEverydayVo.setName(xssfRow.getCell(0).toString());//姓名
            recordEverydayVo.setDept(xssfRow.getCell(2).toString());//部门
            recordEverydayVo.setId(xssfRow.getCell(3).toString());//id
            recordEverydayVo.setPosition(xssfRow.getCell(4).toString());//职位
            recordEverydayVo.setDate(xssfRow.getCell(6).toString());//日期
            recordEverydayVo.setJobTime(xssfRow.getCell(7).toString());//班次
            recordEverydayVo.setAmClockIn(xssfRow.getCell(8).toString());//上班打卡时间
            recordEverydayVo.setAmClockInResult(xssfRow.getCell(9).toString());//上班打卡结果
            recordEverydayVo.setPmClockOut(xssfRow.getCell(10).toString());//下班打卡时间
            recordEverydayVo.setPmClockOutResult(xssfRow.getCell(11).toString());//下班打卡结果
            recordEverydayVo.setAssociatedApproval(xssfRow.getCell(16).toString());//关联的审批单
            recordEverydayVos.add(recordEverydayVo);//保存对象
        }
        return recordEverydayVos;
    }

    /**
     * 将转换好的HashMap转换为excel下载
     */
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
        //要按配置文件中的姓名顺序生成考勤结果,因此需要遍历配置的名字
        String [] splitname=name.split(";");
        //最终使用的结果部分设置底色
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);;
        //关键点 IndexedColors.AQUA.getIndex() 对应颜色
        style.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        for (String personName : splitname) { 
            //用配置的名字从Map里了考勤结果导出对象
            KaoQinGetOutVo kaoQinGetOutVo = kaoQinGetOutMap.get(personName);
            XSSFRow row = sheet.createRow(rowNum);
            XSSFCell cell;
            int index = 0;
            cell = row.createCell(index++);
            cell.setCellValue(kaoQinGetOutVo.getName());// 姓名
            cell = row.createCell(index++);
            cell.setCellStyle(style);
            cell.setCellValue(kaoQinGetOutVo.getDaysOfBusinessTrip().doubleValue());// 出差天数
            cell = row.createCell(index++);
            cell.setCellStyle(style);
            cell.setCellValue(kaoQinGetOutVo.getDinnerAllowanceDays().doubleValue());// 晚餐补助天数
            cell = row.createCell(index++);
            cell.setCellStyle(style);
            cell.setCellValue(kaoQinGetOutVo.getFinalDeductionOfAttendance().doubleValue());// 最终扣减考勤
            cell = row.createCell(index++);
            cell.setCellStyle(style);
            cell.setCellValue(kaoQinGetOutVo.getRemainingAnnualLeaveOfThisMonth().doubleValue());// 本月剩余年假
            cell = row.createCell(index++);
            cell.setCellStyle(style);
            cell.setCellValue(kaoQinGetOutVo.getRestOfThisMonth().doubleValue());// 本月剩余调休
            cell = row.createCell(index++);
            cell.setCellValue(kaoQinGetOutVo.getRemainingAnnualLeaveOfLastMonth().doubleValue());// 上月剩余年假
            cell = row.createCell(index++);
            cell.setCellValue(kaoQinGetOutVo.getRestOfLastMonth().doubleValue());// 上月剩余调休
            cell = row.createCell(index++);
            cell.setCellValue(kaoQinGetOutVo.getOvertimeThisMonth().doubleValue());// 本月加班
            cell = row.createCell(index++);
            cell.setCellValue(kaoQinGetOutVo.getLeaveDaysOfThisMonth().doubleValue());// 本月请假天数
            cell = row.createCell(index++);
            cell.setCellValue(kaoQinGetOutVo.getAnnualLeaveDaysOfThisMonth().doubleValue());// 本月休年假天数
            cell = row.createCell(index++);
            cell.setCellValue(kaoQinGetOutVo.getLateTimesOfThisMonth().doubleValue());// 本月迟到次数
            cell = row.createCell(index++);
            cell.setCellValue(kaoQinGetOutVo.getEarlyLeaveTimesOfThisMonth().doubleValue());// 本月早退次数
            cell = row.createCell(index++);
            cell.setCellValue(kaoQinGetOutVo.getNumberOfAbsenteeismThisMonth().doubleValue());// 本月旷工次数
            cell = row.createCell(index++);
            cell.setCellValue(kaoQinGetOutVo.getDeductionDays().doubleValue());// 违纪扣减天数

            
            rowNum++;
        }

        //以当前时间命名导出的考勤文件
        String fileName = "考勤统计结果_"+
                sdfFilename.format(new Date())+".xlsx";
        if("download".equals(saveType)) {//使用下载的方式
            //清空response  
            response.reset();  
            //设置response的Header  
            try {
                response.addHeader("Content-Disposition", "attachment;filename="+
                        new String(fileName.getBytes(),"iso-8859-1"));
            } catch (UnsupportedEncodingException e2) {
            }  
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
        }else if("save".equals(saveType)) {//使用直接保存的方式
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
        sheet.setColumnWidth(index++, (int)(8.64*256));
        sheet.setColumnWidth(index++, (int)(12.91*256));
        sheet.setColumnWidth(index++, (int)(12.91*256));
        sheet.setColumnWidth(index++, (int)(12.91*256));
        sheet.setColumnWidth(index++, (int)(12.91*256));
        sheet.setColumnWidth(index++, (int)(12.91*256));
        sheet.setColumnWidth(index++, (int)(16.45*256));
        sheet.setColumnWidth(index++, (int)(15.73*256));
        sheet.setColumnWidth(index++, (int)(12.91*256));
        sheet.setColumnWidth(index++, (int)(15.09*256));
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
    }

    /**
     * 计算考勤记录，将考勤文件实体对象转换为将要转换成导出表格的HashMap
     */
    @Override
    public HashMap<String, KaoQinGetOutVo> convertObject(KaoQinExcelMainVo kaoQinExcelMainVo) {
        HashMap<String, KaoQinGetOutVo> kaoQinGetOutMap = new HashMap<>();
        //解析需要读取考勤记录的人员姓名,并存储到map中
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
        //按每日考勤记录转换,计算迟到,早退,加班时间,出差,饭补等数据
        this.calRecordEveryDay(kaoQinGetOutMap,kaoQinExcelMainVo.getRecordEverydayVos());
        //按本月加班申请情况记录统计加班时间
        this.calOvertime(kaoQinGetOutMap,kaoQinExcelMainVo.getThisMonthOvertimeVos());
        //按上月剩余假期情况计算本月剩余假期
        this.calHoliday(kaoQinGetOutMap,kaoQinExcelMainVo.getLastMonthHolidaysVos());
        //计算Map中所有人的最终扣减考勤,剩余调休
        this.calResult(kaoQinGetOutMap);
        return kaoQinGetOutMap;
    }

    /**
     * 计算Map中所有人的最终扣减考勤,剩余调休
     * @param kaoQinGetOutMap
     */
    private void calResult(HashMap<String, KaoQinGetOutVo> kaoQinGetOutMap) {
        for(String personName : kaoQinGetOutMap.keySet()) {
            KaoQinGetOutVo kaoQinGetOutVo = kaoQinGetOutMap.get(personName);
            //定义迟到早退总次数,将每个人的迟到,早退次数相加
            int lateTimes = kaoQinGetOutVo.getLateTimesOfThisMonth().add(kaoQinGetOutVo.getEarlyLeaveTimesOfThisMonth()).intValue();
            double deductionDays = 0;
            if(lateTimes > this.freeBeLateTimes && lateTimes <= this.smallBeLateTimes) {
                //如果是属于一般迟到早退,按一般迟到早退的系数来计算扣减考勤天数
                deductionDays=BigDecimalUtils.add(deductionDays, 
                        BigDecimalUtils.mul(lateTimes-this.freeBeLateTimes, this.freeBlateDays));
            }else if(lateTimes > this.smallBeLateTimes){
                //如果超过了一般迟到早退的最大次数,则按旷工迟到早退的系数来计算扣减考勤天数,并且要加上一般迟到部分的扣减天数
                deductionDays=BigDecimalUtils.add(BigDecimalUtils.
                            add(deductionDays, 
                                    BigDecimalUtils.mul(this.smallBeLateTimes-this.freeBeLateTimes, this.freeBlateDays)),
                        BigDecimalUtils.mul(lateTimes-this.smallBeLateTimes,this.smallBlateDays));
            }
            //违纪扣减天数还要再加上旷工次数乘以系数得到的旷工扣减考勤天数
            deductionDays = BigDecimalUtils.add(deductionDays, kaoQinGetOutVo.getNumberOfAbsenteeismThisMonth().
                    multiply(BigDecimal.valueOf(this.smallBlateDays)).doubleValue());
            kaoQinGetOutVo.setDeductionDays(BigDecimal.valueOf(deductionDays));
            //计算本月加班时长,转换为小时,并按加班转调休比例,转换为累计的调休时长
            double overtimeThisMonth = kaoQinGetOutVo.getOvertimeThisMonth().doubleValue();
            overtimeThisMonth = BigDecimalUtils.div(overtimeThisMonth, 8/this.overtimeRate);
            double restOfLastMonth = kaoQinGetOutVo.getRestOfLastMonth().doubleValue();
            double leaveDaysOfThisMonth = kaoQinGetOutVo.getLeaveDaysOfThisMonth().doubleValue();
            double finalDeductionOfAttendance = 0;
            double restOfThisMonth = 0;
            //计算中间过程,先用上月剩余调休加上本月加班累计得到的调休,减去请假次数乘以2,得到二者差值
            restOfThisMonth = BigDecimalUtils.sub(BigDecimalUtils.add(restOfLastMonth,overtimeThisMonth),
                    BigDecimalUtils.mul(leaveDaysOfThisMonth,0.5));
            //如果差值为正,说明调休时长够用,不需要扣减考勤
            if(restOfThisMonth>=0) {
                finalDeductionOfAttendance = 0;
            }else {
                //否则,调休不够用了,需要判断缺少的时间是否超过1天,如果没超过1天,则不需要扣减考勤
                if(restOfThisMonth>-1) {
                    finalDeductionOfAttendance = 0;
                }else {
                    //如果超过了1天,需要将超出的整数部分计入扣减考勤天数中,小数部分累计到调休时间中
                    finalDeductionOfAttendance = Math.floor(-restOfThisMonth);
                    restOfThisMonth = BigDecimalUtils.add(restOfThisMonth, finalDeductionOfAttendance);
                }
            }
            //将计算好的各项结果保存到导出对象中.
            kaoQinGetOutVo.setRestOfThisMonth(BigDecimal.valueOf(restOfThisMonth));
            kaoQinGetOutVo.setFinalDeductionOfAttendance(BigDecimal.valueOf(finalDeductionOfAttendance).
                    add(kaoQinGetOutVo.getDeductionDays()));
            
            kaoQinGetOutVo.setRemainingAnnualLeaveOfThisMonth(kaoQinGetOutVo.getRemainingAnnualLeaveOfLastMonth().
                    subtract(kaoQinGetOutVo.getAnnualLeaveDaysOfThisMonth()));
            //由于考勤结果里是要显示请假天数，因此需要将请假次数转换为请假天数
            kaoQinGetOutVo.setLeaveDaysOfThisMonth(kaoQinGetOutVo.getLeaveDaysOfThisMonth().
                    multiply(BigDecimal.valueOf(0.5)));
        }
    }

    /**
     * 按上月剩余假期情况计算本月剩余假期
     * @param kaoQinGetOutMap
     * @param lastMonthHolidaysVos
     */
    private void calHoliday(HashMap<String, KaoQinGetOutVo> kaoQinGetOutMap,
            ArrayList<LastMonthHolidaysVo> lastMonthHolidaysVos) {
        for (Iterator<LastMonthHolidaysVo> iterator = lastMonthHolidaysVos.iterator(); iterator.hasNext();) {
            LastMonthHolidaysVo lastMonthHolidaysVo = iterator.next();
            if(kaoQinGetOutMap.containsKey(lastMonthHolidaysVo.getName())) {
                KaoQinGetOutVo kaoQinGetOutVo = kaoQinGetOutMap.get(lastMonthHolidaysVo.getName());
                //将上月剩余假期直接保存到对象里
                kaoQinGetOutVo.setRestOfLastMonth(kaoQinGetOutVo.getRestOfLastMonth().
                        add(new BigDecimal(lastMonthHolidaysVo.getOvertimeHours())));
                kaoQinGetOutVo.setRemainingAnnualLeaveOfLastMonth(kaoQinGetOutVo.getRemainingAnnualLeaveOfLastMonth().
                        add(new BigDecimal(lastMonthHolidaysVo.getAnnualLeave())));
            }
        }
    }

    /**
     * 按本月加班申请情况记录统计加班时间
     * @param kaoQinGetOutMap
     * @param thisMonthOvertimeVos
     */
    private void calOvertime(HashMap<String, KaoQinGetOutVo> kaoQinGetOutMap,
            ArrayList<ThisMonthOvertimeVo> thisMonthOvertimeVos) {
        for (Iterator<ThisMonthOvertimeVo> iterator = thisMonthOvertimeVos.iterator(); iterator.hasNext();) {
            ThisMonthOvertimeVo thisMonthOvertimeVo = iterator.next();
            if(kaoQinGetOutMap.containsKey(thisMonthOvertimeVo.getName())) {
                KaoQinGetOutVo kaoQinGetOutVo = kaoQinGetOutMap.get(thisMonthOvertimeVo.getName());
                //直接按加班申请中的时间累计到加班时长里
                kaoQinGetOutVo.setOvertimeThisMonth(kaoQinGetOutVo.getOvertimeThisMonth().
                        add(new BigDecimal(thisMonthOvertimeVo.getOvertimeHours())));
            }
        }
        
    }

    /**
     * 按每日考勤记录转换,计算迟到,早退,加班时间,出差,饭补等数据
     * @param kaoQinGetOutMap
     * @param recordEverydayVos
     */
    @SuppressWarnings("deprecation")
    private void calRecordEveryDay(HashMap<String, KaoQinGetOutVo> kaoQinGetOutMap,
            ArrayList<RecordEverydayVo> recordEverydayVos) {
        //初始化标准上班打卡时间,将配置中的分号换为冒号
        Date standardClockInDate = this.convertTime(this.standardClockIn.replace(";", ":"));
        //初始化标准下班打卡时间,将配置中的分号换为冒号
        Date standardClockOutDate = this.convertTime(this.standardClockOut.replace(";", ":"));
        //初始化晚餐补助开始时间,将配置中的分号换为冒号
        Date subsidyStartTimeDate = this.convertTime(this.subsidyStartTime.replace(";", ":"));
        //初始化加班时间开始计算的时间,将配置中的分号换为冒号
        Date overtimeStartTimeDate = this.convertTime(this.overtimeStartTime.replace(";", ":"));
        //开始遍历每一条考勤记录
        for (Iterator<RecordEverydayVo> iterator = recordEverydayVos.iterator(); iterator.hasNext();) {
            RecordEverydayVo recordEverydayVo = iterator.next();
            //不在待统计的人员名单中的人员不需要统计
            if(kaoQinGetOutMap.containsKey(recordEverydayVo.getName())) {
                KaoQinGetOutVo kaoQinGetOutVo = kaoQinGetOutMap.get(recordEverydayVo.getName());
                Date nowDate = this.convertDate(recordEverydayVo.getDate());
                //如果工作日,走工作日的相关计算逻辑
                if(this.isWorkDay(nowDate)) {
                    if(recordEverydayVo.getAmClockInResult().contains("请假")) {
                        //计算请假天数
                        if(recordEverydayVo.getAssociatedApproval().contains("年假")) {
                            //年假的审批单里有年假字样,以此判断此假期为年假,计算到年假天数里,考勤表中的一次年假计0.5天年假
                            kaoQinGetOutVo.setAnnualLeaveDaysOfThisMonth(kaoQinGetOutVo.getAnnualLeaveDaysOfThisMonth().
                                    add(BigDecimal.valueOf(0.5)));
                        }else {//否则就是一般的请假，这里记录的是请假次数，后面转换为天数的时候会再除以2作转换
                            kaoQinGetOutVo.setLeaveDaysOfThisMonth(kaoQinGetOutVo.getLeaveDaysOfThisMonth().
                                    add(BigDecimal.valueOf(1)));
                        }
                    }
                    if(recordEverydayVo.getPmClockOutResult().contains("请假")) {
                        //年假的审批单里有年假字样,以此判断此假期为年假,计算到年假天数里,考勤表中的一次年假计0.5天年假
                        if(recordEverydayVo.getAssociatedApproval().contains("年假")) {
                            kaoQinGetOutVo.setAnnualLeaveDaysOfThisMonth(kaoQinGetOutVo.getAnnualLeaveDaysOfThisMonth().
                                    add(BigDecimal.valueOf(0.5)));
                        }else {//否则就是一般的请假，这里记录的是请假次数，后面转换为天数的时候会再除以2作转换
                            kaoQinGetOutVo.setLeaveDaysOfThisMonth(kaoQinGetOutVo.getLeaveDaysOfThisMonth().
                                    add(BigDecimal.valueOf(1)));
                        }
                    }
                    //计算出差天数
                    if(recordEverydayVo.getPmClockOutResult().contains("出差")
                            ||recordEverydayVo.getAmClockInResult().contains("出差")) {
                        kaoQinGetOutVo.setDaysOfBusinessTrip(kaoQinGetOutVo.getDaysOfBusinessTrip().
                                add(BigDecimal.valueOf(1)));
                    }
                    //计算考勤异常情况
                    String amClockIn = recordEverydayVo.getAmClockIn();
                    if(recordEverydayVo.getAmClockInResult().contains("迟到")||
                            recordEverydayVo.getAmClockInResult().contains("缺卡")) {
                        if(amClockIn.contains(":")) {//有打卡时间代码上班打过卡了
                            Date dateClockIn = this.convertTime(amClockIn);
                            //计算打打卡时间与标准打卡时间的时间差
                            long diffMinute = (dateClockIn.getTime()-standardClockInDate.getTime())/1000/60;
                            if(diffMinute<beLateLimitMinute) {//如果时间差小于配置的一般迟到时长的上限,则统计为一般迟到
                                kaoQinGetOutVo.setLateTimesOfThisMonth(kaoQinGetOutVo.getLateTimesOfThisMonth().
                                        add(BigDecimal.valueOf(1)));
                            }else {//否则统计为旷工
                                kaoQinGetOutVo.setNumberOfAbsenteeismThisMonth(kaoQinGetOutVo.getNumberOfAbsenteeismThisMonth().
                                        add(BigDecimal.valueOf(1)));
                            }
                        }else {//没有打卡时间直接统计为旷工
                            kaoQinGetOutVo.setNumberOfAbsenteeismThisMonth(kaoQinGetOutVo.getNumberOfAbsenteeismThisMonth().
                                    add(BigDecimal.valueOf(1)));
                        }
                    }
                    String pmClockOut = recordEverydayVo.getPmClockOut();
                    if(recordEverydayVo.getPmClockOutResult().contains("早退")||
                            recordEverydayVo.getPmClockOutResult().contains("缺卡")) {
                        if(pmClockOut.contains(":")) {//有打卡时间代码下班打过卡了
                            Date dateClockOut = this.convertTime(pmClockOut);
                            //计算打打卡时间与标准打卡时间的时间差
                            long diffMinute = (standardClockOutDate.getTime()-dateClockOut.getTime())/1000/60;
                            //卢玥现在哺乳假，不计早退
                            if(!"卢玥".equals(recordEverydayVo.getName())){
                                if(diffMinute<beLateLimitMinute) {//如果时间差小于配置的一般早退时长的上限,则统计为一般早退
                                    kaoQinGetOutVo.setEarlyLeaveTimesOfThisMonth(kaoQinGetOutVo.getEarlyLeaveTimesOfThisMonth().
                                            add(BigDecimal.valueOf(1)));
                                }else {//否则统计为早退
                                    kaoQinGetOutVo.setNumberOfAbsenteeismThisMonth(kaoQinGetOutVo.getNumberOfAbsenteeismThisMonth().
                                            add(BigDecimal.valueOf(1)));
                                }
                            }
                        }else {//没有打卡时间直接统计为旷工
                            kaoQinGetOutVo.setNumberOfAbsenteeismThisMonth(kaoQinGetOutVo.getNumberOfAbsenteeismThisMonth().
                                    add(BigDecimal.valueOf(1)));
                        }
                    }else if (recordEverydayVo.getPmClockOutResult().contains("正常")||
                            recordEverydayVo.getPmClockOutResult().contains("补卡")||
                            recordEverydayVo.getPmClockOutResult().contains("出差")) {
                        if(pmClockOut.contains(":")) {
                            Date dateClockOut = this.convertTime(pmClockOut);
                            //正常和补卡打卡情况下,如果下班打卡时间超过了晚餐补助的起始时间,则统计一天晚餐补助
                            if(subsidyStartTimeDate.getTime()-dateClockOut.getTime()<0) {
                                kaoQinGetOutVo.setDinnerAllowanceDays(kaoQinGetOutVo.getDinnerAllowanceDays().
                                        add(BigDecimal.valueOf(1)));
                            }
                            //正常和补卡打卡情况下,如果下班打卡时间超过了晚上加班累计调休的开始时间,则开始计算当天的调休时间
                            //先计算时间差
                            long diffMinute = (dateClockOut.getTime() - overtimeStartTimeDate.getTime())/1000/60;
                            //如果时间差是负的,则置为0
                            if(diffMinute<0){
                                diffMinute = 0;
                            }
                            //将分钟转换成小时
                            double diffHour = Math.floorDiv(diffMinute, 60);
                            //保存到对象中,单位为小时
                            kaoQinGetOutVo.setOvertimeThisMonth(kaoQinGetOutVo.getOvertimeThisMonth().
                                    add(BigDecimal.valueOf(diffHour)));
                        }
                    }
                }else {//非工作日则按非工作日直接按下班时间减去上班时间再减一个小时计为加班时长
                    String amClockIn = recordEverydayVo.getAmClockIn();
                    String amClockOut = recordEverydayVo.getPmClockOut();
                    if(amClockIn.contains(":")&&amClockOut.contains(":")) {
                        Date dateClockIn = this.convertTime(amClockIn);
                        Date dateClockOut = this.convertTime(amClockOut);
                        //先计算时间差,因为是非工作日,要去掉中午吃饭的一个小时,全天按8小时计算
                        double diffHour = (dateClockOut.getTime()-dateClockIn.getTime())/1000/60/60-1;
                        //如果时间差是负的,则置为0
                        if(diffHour<0) {
                            diffHour = 0;
                        }
                        //如果加班当天为1号月结,则将加班时间要按1:1累计调休,所以这里先除以加班累计调休比例
                        if(nowDate.getDay()==1) {
                            diffHour = diffHour/this.overtimeRate;
                        }
                        //保存非工作日的加班时长
                        kaoQinGetOutVo.setOvertimeThisMonth(kaoQinGetOutVo.getOvertimeThisMonth().
                                add(BigDecimal.valueOf(diffHour)));
                    }
                }
            }
        }
    }
    /**
     * 将09:00格式的字符串转换成日期对象
     * @param time
     * @return
     */
    private Date convertTime(String time) {
        if(time.contains(":")) {
            Calendar calendar = Calendar.getInstance();
            calendar.clear();
            String hour = time.split(":")[0];
            String minute = time.split(":")[1];
            if(hour.contains("次日 ")){
                calendar.set(Calendar.YEAR,1970);
                calendar.set(Calendar.MONTH,0);
                calendar.set(Calendar.DAY_OF_MONTH,2);
                hour = hour.replace("次日 ","");
            }
            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour));
            calendar.set(Calendar.MINUTE, Integer.parseInt(minute));
            return calendar.getTime();
        }else {
            return null;
        }
    }

    /**
     * 将19-11-01 星期一格式的字符串转换成日期对象
     * @param date
     * @return
     */
    private Date convertDate(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return sdf.parse("20"+date.split(" ")[0]);
        } catch (ParseException e) {
            return null;
        }
    }
    /**
     * 判断传入日期是否为工作日
     * @param checkDate
     * @return
     */
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
        //如果不是特殊工作日，则按星期几判断,7是周六,1是周日
        if(week == 1 || week == 7) {//周六日为非工作日
            result = false;
        }else {//周一到周五为工作日
            result = true;
        }
        return result;
    }
}
