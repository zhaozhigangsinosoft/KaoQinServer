package cn.kaoqin.controller;

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
     * 转换考勤文件请求入口
     * @return
     */
    @RequestMapping(value = "/convert/{saveType}")
    public String convertExcel(@PathVariable("saveType") String saveType) {
        //此处可以选择save和download两种方式，save为直接将文件生成到本地路径下，download为通过浏览器下载
        if(!RegTest.match(saveType, "^(save|download)$")) {
            return "Error export method!Please enter \"save\""
                    + " or \"download\" in the URL!";
        }
        //解析考勤原始文件为实体对象
        KaoQinExcelMainVo kaoQinExcelMainVo = kaoQinService.importExcel(filePath);
        //将考勤文件实体对象转换为将要转换成导出表格的HashMap
        HashMap<String,KaoQinGetOutVo> KaoQinGetOutMap = kaoQinService.convertObject(kaoQinExcelMainVo);
        try {
            //将转换好的HashMap转换为excel下载
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
