package cn.kaoqin.service;

import java.util.ArrayList;

import javax.servlet.http.HttpServletResponse;

import cn.kaoqin.vo.KaoQinRecordVo;

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
    public void exportExcel(ArrayList<KaoQinRecordVo> kaoqinRecordVoList
            , HttpServletResponse response
            , String saveType, String filePath);
}
