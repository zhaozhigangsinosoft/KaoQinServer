package cn.kaoqin.impl;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cn.kaoqin.service.KaoQinService;
import cn.kaoqin.vo.KaoQinRecordVo;
import cn.util.FileUtils;

/**
 * 考勤文件处理服务接口实现类
 * @author ZhaoZhigang
 *
 */
@Service
public class KaoQinServiceImpl implements KaoQinService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 将考勤文件列表转换为excel文件并存到response中进行下载
     * @param kaoqinRecordVoList
     * @param response
     * @param saveType 
     * @param filePath 
     */
    public void exportExcel(ArrayList<KaoQinRecordVo> kaoqinRecordVoList
            , HttpServletResponse response
            , String saveType, String filePath){
        XSSFWorkbook workbook = new XSSFWorkbook();
        //创建支出sheet页
        XSSFSheet sheetSupport = workbook.createSheet("支出");
        //创建收入sheet页
        XSSFSheet sheetCollection = workbook.createSheet("收入");
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        SimpleDateFormat sdfFilename = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");

        //创建支出页表头
        this.setTitleSupport(workbook, sheetSupport);
        //创建收入页表头
        this.setTitleCollection(workbook, sheetCollection);

        //定义支出页行索引
        int rowNumSupport = 1;
        //定义收入页行索引
        int rowNumCollection = 1;
        for (KaoQinRecordVo kaoqinRecordVo:kaoqinRecordVoList) {
            if(kaoqinRecordVo.getCollectionOrSupport().equals("支出")) {
                //如果对象收支属性为支出，则按支出页格式设置表格数据
                XSSFRow row = sheetSupport.createRow(rowNumSupport);
                int index = 0;
                row.createCell(index++).setCellValue(
                        kaoqinRecordVo.getExpenditureCategories());
                row.createCell(index++).setCellValue(
                        kaoqinRecordVo.getExpenditureCategory());
                row.createCell(index++).setCellValue(
                        kaoqinRecordVo.getRecord());
                row.createCell(index++).setCellValue(
                        kaoqinRecordVo.getCurrency());
                row.createCell(index++).setCellValue(
                        kaoqinRecordVo.getProject());
                row.createCell(index++).setCellValue(
                        kaoqinRecordVo.getBusiness());
                row.createCell(index++).setCellValue(
                        kaoqinRecordVo.getReimbursement());
                row.createCell(index++).setCellValue(
                        sdf.format(kaoqinRecordVo.getConsumptionDate()));
                //设置交易金额单元格格式
                XSSFCell cell = row.createCell(index++);
                XSSFDataFormat df = workbook.createDataFormat();
                XSSFCellStyle contextstyle =workbook.createCellStyle();
                contextstyle.setDataFormat(df.getFormat("#,##0.00"));
                cell.setCellStyle(contextstyle);
                cell.setCellValue(
                        kaoqinRecordVo.getConsumptionAmount().doubleValue());
                
                row.createCell(index++).setCellValue(
                        kaoqinRecordVo.getMemberAmount());
                row.createCell(index++).setCellValue(
                        kaoqinRecordVo.getRemarks());
                row.createCell(index++).setCellValue(
                        kaoqinRecordVo.getRecordBook());
                rowNumSupport++;
            }else if(kaoqinRecordVo.getCollectionOrSupport().equals("收入")) {
                //如果对象收支属性为支出，则按支出页格式设置表格数据
                XSSFRow row = sheetCollection.createRow(rowNumCollection);
                int index = 0;
                row.createCell(index++).setCellValue(
                        kaoqinRecordVo.getExpenditureCategories());
                row.createCell(index++).setCellValue(
                        kaoqinRecordVo.getRecord());
                row.createCell(index++).setCellValue(
                        kaoqinRecordVo.getCurrency());
                row.createCell(index++).setCellValue(
                        kaoqinRecordVo.getProject());
                row.createCell(index++).setCellValue("");
                row.createCell(index++).setCellValue(
                        sdf.format(kaoqinRecordVo.getConsumptionDate()));
                //设置交易金额单元格格式
                XSSFCell cell = row.createCell(index++);
                XSSFDataFormat df = workbook.createDataFormat();
                XSSFCellStyle contextstyle =workbook.createCellStyle();
                contextstyle.setDataFormat(df.getFormat("#,##0.00"));
                cell.setCellStyle(contextstyle);
                cell.setCellValue(
                        kaoqinRecordVo.getConsumptionAmount().doubleValue());
                row.createCell(index++).setCellValue(
                        kaoqinRecordVo.getMemberAmount());
                row.createCell(index++).setCellValue(
                        kaoqinRecordVo.getRemarks());
                row.createCell(index++).setCellValue(
                        kaoqinRecordVo.getRecordBook());
                rowNumCollection++;
            }
        }
        //以当前时间命名导出的考勤文件
        String fileName = "exportWacaiRecord_"+
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
    
    /**
     * 设置支出页表头
     * @param workbook
     * @param sheet
     */
    private void setTitleSupport(XSSFWorkbook workbook, XSSFSheet sheet){
        XSSFRow row = sheet.createRow(0);
        //设置列宽，setColumnWidth的第二个参数要乘以256，这个参数的单位是1/256个字符宽度
        int index = 0;
        sheet.setColumnWidth(index++, 10*256);
        sheet.setColumnWidth(index++, 10*256);
        sheet.setColumnWidth(index++, 20*256);
        sheet.setColumnWidth(index++, 10*256);
        sheet.setColumnWidth(index++, 10*256);
        sheet.setColumnWidth(index++, 10*256);
        sheet.setColumnWidth(index++, 10*256);
        sheet.setColumnWidth(index++, 20*256);
        sheet.setColumnWidth(index++, 10*256);
        sheet.setColumnWidth(index++, 15*256);
        sheet.setColumnWidth(index++, 50*256);
        sheet.setColumnWidth(index++, 10*256);

        //设置为居中加粗，红色
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setColor(XSSFFont.COLOR_RED);
        style.setFont(font);

        XSSFCell cell;
        index = 0;
        cell = row.createCell(index++);
        cell.setCellValue("支出大类");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("支出小类");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("账户");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("币种");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("项目");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("商家");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("报销");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("消费日期");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("消费金额");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("成员金额");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("备注");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("考勤");
        cell.setCellStyle(style);
    }
    
    /**
     * 设置收入页表头
     * @param workbook
     * @param sheet
     */
    private void setTitleCollection(XSSFWorkbook workbook, XSSFSheet sheet){
        XSSFRow row = sheet.createRow(0);
        //设置列宽，setColumnWidth的第二个参数要乘以256，这个参数的单位是1/256个字符宽度
        int index = 0;
        sheet.setColumnWidth(index++, 10*256);
        sheet.setColumnWidth(index++, 10*256);
        sheet.setColumnWidth(index++, 10*256);
        sheet.setColumnWidth(index++, 10*256);
        sheet.setColumnWidth(index++, 10*256);
        sheet.setColumnWidth(index++, 20*256);
        sheet.setColumnWidth(index++, 10*256);
        sheet.setColumnWidth(index++, 15*256);
        sheet.setColumnWidth(index++, 50*256);
        sheet.setColumnWidth(index++, 10*256);
        
        //设置为居中加粗，红色
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setColor(XSSFFont.COLOR_RED);
        style.setFont(font);
        
        XSSFCell cell;
        index = 0;
        cell = row.createCell(index++);
        cell.setCellValue("收入大类");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("账户");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("币种");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("项目");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("付款方");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("收入日期");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("收入金额");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("成员金额");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("备注");
        cell.setCellStyle(style);
        cell = row.createCell(index++);
        cell.setCellValue("考勤");
        cell.setCellStyle(style);
    }
}
