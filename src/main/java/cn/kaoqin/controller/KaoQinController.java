package cn.kaoqin.controller;

import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.kaoqin.service.KaoQinService;
import cn.kaoqin.vo.KaoQinExcelMainVo;
import cn.kaoqin.vo.KaoQinGetOutVo;
import cn.kaoqin.vo.RecordEverydayVo;
import cn.util.RegTest;

/**
 * 考勤转换功能控制器类
 * @author ZhaoZhigang
 *
 */
@RestController
@RequestMapping("/kaoqin")
public class KaoQinController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    //配置文件中获取考勤文件读取路径
    @Value("${params.kaoqin.filepath}")
    private String filePath;
    
    @Autowired
    private KaoQinService kaoQinService;
    
    @Autowired
    private HttpServletResponse response;
    
    /**
     * 转换考勤文件请求服务
     * @return
     */
    @RequestMapping(value = "/convert/{saveType}")
    public String convertExcel(@PathVariable("saveType") String saveType) {
        if(!RegTest.match(saveType, "^(save|download)$")) {
            return "Error export method!Please enter \"save\""
                    + " or \"download\" in the URL!";
        }
        
        KaoQinExcelMainVo kaoQinExcelMainVo = kaoQinService.importExcel(filePath);
        HashMap<String,KaoQinGetOutVo> KaoQinGetOutMap = kaoQinService.convertObject(kaoQinExcelMainVo);
        try {
            //将考勤对象转换为excel下载
            if(!KaoQinGetOutMap.isEmpty()) {
                kaoQinService.exportExcel(
                        KaoQinGetOutMap, response, saveType ,filePath);
            }else {
                return "No records!";
            }
            
        } catch (Exception e2) {
            //如果转换发生异常则返回失败
            logger.error(e2.getMessage(),e2);
            return "Failed";
        }
        return "Success";
        
    }
}
