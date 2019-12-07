/**
 * copyfuture.com
 * Copyright (C) 2013-2018 All Rights Reserved.
 */
package com.uifuture.chinaipfilter.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 定制的文件工具类
 *
 * @author chenhx
 * @version FileUtils.java, v 0.1 2018-09-03 下午 7:24
 */
public class FileUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);
    /**
     * 递归取到当前目录所有文件
     *
     * @param dir
     * @return
     */
    public static List<String> getFilesName(String dir) {
        List<String> lstFiles = new ArrayList<>();
        File[] files = new File(dir).listFiles();
        if (files == null) {
            return lstFiles;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                lstFiles.addAll(getFilesName(f.getAbsolutePath()));
            } else {
                lstFiles.add(f.getAbsolutePath());
            }
        }
        return lstFiles;
    }


    /**
     * 递归取到当前目录指定数量的文件
     *
     * @param dir
     * @return
     */
    public static List<String> getFilesName(String dir, Integer size) {
        List<String> lstFiles = new ArrayList<>();
        File[] files = new File(dir).listFiles();
        if (files == null) {
            return lstFiles;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                lstFiles.addAll(getFilesName(f.getAbsolutePath(), size));
            } else {
                lstFiles.add(f.getAbsolutePath());
            }
            if (lstFiles.size() > size) {
                return lstFiles;
            }
        }
        return lstFiles;
    }

    /**
     * 创建目录
     *
     * @param destFilePath
     * @return
     */
    public static boolean createDir(String destFilePath) {
        File file = new File(destFilePath);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 移除路径下文件
     *
     * @param path
     */
    public static void removeFiles(String path) {
        File file = new File(path);
        if (file.exists()) {
            deleteFile(file);
        }
    }

    /**
     * 删除文件
     *
     * @param file
     */
    public static Boolean deleteFile(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File file1 : files) {
                    deleteFile(file1);
                }
            }
        }
        return file.delete();
    }


    /**
     * 获取resource路径下的文件
     * 按行读取字符串
     * @param path
     * @return
     */
    public static List<String> readListFromResourceFile(String path) {
        List<String> stringList = new ArrayList<String>(1024);
        readCollectionFromResourceFile(path, stringList);
        return stringList;
    }

    private static void readCollectionFromResourceFile(String path, Collection<String> collection) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(FileUtils.class.getClassLoader().getResourceAsStream(path)));
            for (String buf = ""; (buf = br.readLine()) != null; ) {
                if ("".equals(buf.trim())) {
                    continue;
                }
                collection.add(buf);
            }
        } catch (Exception e) {
            LOGGER.error("[FileUtils-readListFromResourceFile]获取resource路径下的文件出现异常,path="+path,e);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
            }
        }
    }

    /**
     * 获取resource路径下的文件
     * 按行读取字符串
     * @param path
     * @return
     */
    public static Set<String> readSetFromResourceFile(String path) {
        Set<String> stringSet = new HashSet<>(1024);
        readCollectionFromResourceFile(path, stringSet);
        return stringSet;
    }

    /**
     * 将字符串保存到文件中去
     * resource路径下的文件
     * 注意：这是保存到class目录下去了
     * @param ips
     * @param path
     */
    public static void saveSetToResourceFile(Collection<String> ips, String path) {
        try {
            String path2 = ResourceUtils.getURL("classpath:").getPath();
            path = path2 +path;
            // 获取文件
            File file = new File(path);
            //如果没有文件就创建
            if (!file.isFile()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(path, true);
            BufferedWriter writer = new BufferedWriter(fw);
            for (String ip : ips) {
                // 往已有的文件上添加字符串
                writer.write(ip + "\r\n");
            }
            writer.close();
            fw.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }
}
