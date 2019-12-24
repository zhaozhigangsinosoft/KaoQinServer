package cn.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * 文件工具类
 * @author ZhaoZhigang
 *
 */
public class FileUtils {
    /**
     * 获取目录路径下所有文件的静态方法
     * @param path 目录路径
     * @param scanSub 是否扫描子文件夹
     * @return ArrayList<File> File对象列表
     * @throws Exception
     */
    public static ArrayList<File> getFiles(String path,boolean scanSub) 
            throws Exception {
        ArrayList<File> fileList = new ArrayList<File>();
        File file = new File(path);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File fileIndex : files) {
                // 如果这个文件是目录，并且入参是需要扫描子文件夹，则进行递归搜索
                if (fileIndex.isDirectory()&&scanSub) {
                        fileList.addAll(getFiles(fileIndex.getPath(),true));
                } else {
                    // 如果文件是普通文件，则将文件对象放入集合中
                    fileList.add(fileIndex);
                }
            }
        }
        return fileList;
    }

    /**
     * 判断文件是否存在，如果不存在则新建
     * 
     * @param filePath 文件路径
     * @throws IOException 
     */
    public static void createNewFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            file.createNewFile();
        }
    }
}
