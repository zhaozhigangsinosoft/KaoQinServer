package cn.kaoqin.service;

import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.HttpServletResponse;

import cn.kaoqin.vo.KaoQinExcelMainVo;
import cn.kaoqin.vo.KaoQinGetOutVo;
import cn.kaoqin.vo.RecordEverydayVo;

/**
 * 考勤文件处理服务接口
 * @author ZhaoZhigang
 *
 */
public interface KaoQinService {
    /**
     * 将考勤文件列表转换为excel文件并存到response中进行下载
     * @param kaoqinRecordVoList
     * @param response
     * @param saveType 
     * @param filePath 
     */
    public void exportExcel(HashMap<String, KaoQinGetOutVo> kaoQinGetOutMap
            , HttpServletResponse response
            , String saveType, String filePath);
    /**
     * 从路径下解析考勤表excel
     * @param filePath
     * @return
     */
    public KaoQinExcelMainVo importExcel(String filePath); 
    
    /**
     * 计算考勤记录，将考勤表对象转换成输出对象
     * @param kaoQinExcelMainVo
     * @return
     */
    public HashMap<String, KaoQinGetOutVo> convertObject(KaoQinExcelMainVo kaoQinExcelMainVo);
    
    
}
