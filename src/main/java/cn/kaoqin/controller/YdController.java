package cn.kaoqin.controller;

import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;

@RestController
@RequestMapping("/yd")
public class YdController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @RequestMapping(value = "/convert")
    public String convertExcel() {
        FileInputStream input = null;
        try {

            File file = new File("D:\\SVN_Folder\\天津SVN\\Development(开发库)\\英大再保\\" +
                    "储备项目\\统计报表项目文件\\" +
                    "FI-YDPIC-REQ-20200402-003再保统计报表开发数据结构设计文档.xlsx");
            logger.info("正在处理文件：" + file.getName());
            XSSFWorkbook wb = null;
            input = new FileInputStream(file);
            // 创建文档
            wb = new XSSFWorkbook(input);
            StringBuffer sbCreate = new StringBuffer();
            StringBuffer sbKey= new StringBuffer();
            StringBuffer sbDrop= new StringBuffer();

            // 解析公式结果
            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
            int startPage = 4;
            int count = 48;
            for(int i = startPage ; i<= startPage+count-1 ; i++){
                XSSFSheet xssfSheet = wb.getSheetAt(i);
                for (int j = 0; j <= xssfSheet.getLastRowNum(); j++) {
                    int index = 0;
                    XSSFRow xssfRow = xssfSheet.getRow(j);
                    if(xssfRow!=null){
                        for(int k = 0; k <= 5; k++){
                            if(xssfRow.getCell(k) != null){
                                CellValue cellValue = evaluator.evaluate(xssfRow.getCell(k));
                                if(cellValue!=null){
                                    sbCreate.append(cellValue.getStringValue());
                                    sbCreate.append(" ");
                                }
                            }
                            index++;
                        }
                        sbCreate.append("\n");

                        index++;
                        if(xssfRow.getCell(index) != null){
                            CellValue cellValue = evaluator.evaluate(xssfRow.getCell(index));
                            if(cellValue!=null){
                                sbKey.append(cellValue.getStringValue());
                                sbKey.append(" ");
                            }
                        }
                        index++;
                        sbKey.append("\n");

                        index++;
                        if(xssfRow.getCell(index) != null){
                            CellValue cellValue = evaluator.evaluate(xssfRow.getCell(index));
                            if(cellValue!=null){
                                sbDrop.append(cellValue.getStringValue());
                                sbDrop.append(" ");
                            }
                        }
                        index++;
                        sbDrop.append("\n");
                    }
                }
            }

            File outputFile = new File("D:\\SVN_Folder\\天津SVN\\Development(开发库)\\英大再保\\" +
                    "储备项目\\统计报表项目文件\\source\\数据库初始化脚本.sql");
            FileWriter fileWriter = null;
            BufferedWriter bw = null;
            try {
                fileWriter = new FileWriter(outputFile);
                bw = new BufferedWriter(fileWriter);
                bw.write(sbDrop.toString());
                bw.write(sbCreate.toString());
                bw.write(sbKey.toString());
                bw.flush();
                bw.close();
            }
            catch(Exception e) {
                logger.error(e.getMessage(),e);
            }finally {
                try {
                    if(fileWriter!=null){
                        fileWriter.close();
                    }
                    if(bw!=null){
                        bw.close();
                    }
                }catch (Exception e){
                    logger.error(e.getMessage(),e);
                }
            }
        }catch (Exception e){
            logger.error(e.getMessage(),e);
        }finally {
            try{
                if(input!=null){
                    input.close();
                }
            }catch (Exception e){
                logger.error(e.getMessage(),e);
            }
        }

        return "Success";
    }
}
