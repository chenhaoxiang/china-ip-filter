/*
 * uifuture.com
 * Copyright (C) 2013-2019 All Rights Reserved.
 */
package com.uifuture.chinaipfilter;


import com.uifuture.chinaipfilter.util.FileUtils;
import org.apache.commons.net.util.SubnetUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 判断某个ip地址是否为中国大陆ip地址。
 * apnic地址段
 * @author chenhx
 * @version ChinaIPRecognizer.java, v 0.1 2019-12-03 23:14 chenhx
 */
public class ChinaIPRecognizer {
    /**
     * 取模
     */
    private static final long RANGE_SIZE = 10000000L;
    /**
     * 分批存储
     */
    private static Map<Integer, List<ChinaRecord>> recordMap = new HashMap<>();
    private static final String CHINA_IP_PATH = "china_ip/ip_long.txt";

    final static class ChinaRecord {
        /**
         * 起始IP
         */
        public long start;
        /**
         * start之后往后多少都是该范围
         */
        public int count;

        private ChinaRecord(long start, int count) {
            this.start = start;
            this.count = count;
        }

        /**
         * 判断ipValue是否在范围内
         * @param ipValue
         * @return
         */
        public boolean contains(long ipValue) {
            return ipValue >= start && ipValue <= start + count;
        }
    }

    static {
        List<ChinaRecord> list = new ArrayList<>();
        //加载文件
        Set<String> ipSet = FileUtils.readSetFromResourceFile(CHINA_IP_PATH);
        for (String ip : ipSet) {
            String[] ipS = ip.split(",");
            long ipLong = Long.parseLong(ipS[0]);
            int count = Integer.parseInt(ipS[1]);
            ChinaRecord chinaRecord = new ChinaRecord(ipLong,count);
            list.add(chinaRecord);
        }

        list.forEach(r -> {
            int key1 = (int) (r.start / RANGE_SIZE);
            int key2 = (int) ((r.start + r.count) / RANGE_SIZE);
            List<ChinaRecord> key1List = recordMap.getOrDefault(key1, new ArrayList<>());
            key1List.add(r);
            recordMap.put(key1, key1List);
            if (key2 > key1) {
                List<ChinaRecord> key2List = recordMap.getOrDefault(key2, new ArrayList<>());
                key2List.add(r);
                recordMap.put(key2, key2List);
            }
        });
    }

    /**
     * 判断是否是大陆IP
     * @param ip
     * @return
     */
    public static boolean isCNIP(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }

        if (isValidIpV4Address(ip)) {
            long value = ipToLong(ip);
            int key = (int) (value / RANGE_SIZE);
            if (recordMap.containsKey(key)) {
                List<ChinaRecord> list = recordMap.get(key);
                return list.stream().anyMatch((ChinaRecord r) -> r.contains(value));
            }
        }

        return false;
    }

    /**
     * 判断字段串是否是有效的IPV4地址。
     *
     * @return true-IPV4地址
     */
    public static boolean isValidIpV4Address(String value) {

        int periods = 0;
        int i;
        int length = value.length();

        if (length > 15) {
            return false;
        }
        char c;
        StringBuilder word = new StringBuilder();
        for (i = 0; i < length; i++) {
            c = value.charAt(i);
            if (c == '.') {
                periods++;
                if (periods > 3) {
                    return false;
                }
                if (word.length() == 0) {
                    return false;
                }
                if (Integer.parseInt(word.toString()) > 255) {
                    return false;
                }
                word.delete(0, word.length());
            } else if (!Character.isDigit(c)) {
                return false;
            } else {
                if (word.length() > 2) {
                    return false;
                }
                word.append(c);
            }
        }

        if (word.length() == 0 || Integer.parseInt(word.toString()) > 255) {
            return false;
        }

        return periods == 3;
    }

    /**
     * 将IP转换为数字存储
     * @param ipAddress
     * @return
     */
    public static long ipToLong(String ipAddress) {
        String[] addrArray = ipAddress.split("\\.");

        long num = 0;
        for (int i = 0; i < addrArray.length; i++) {
            int power = 3 - i;
            // 1. (192 % 256) * 256 pow 3
            // 2. (168 % 256) * 256 pow 2
            // 3. (2 % 256) * 256 pow 1
            // 4. (1 % 256) * 256 pow 0
            num += ((Integer.parseInt(addrArray[i]) % 256 * Math.pow(256, power)));
        }
        return num;
    }

    /**
     * 数字转换为IP格式字符串
     * @param i
     * @return
     */
    public static String longToIp(long i) {
        return ((i >> 24) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + (i & 0xFF);
    }

    /**
     * 将CIDR格式IP块解析出范围内的所有IP地址
     * @param ip
     * @return
     */
    public static String[] analysisCidrIp(String ip){
        SubnetUtils utils = new SubnetUtils(ip);
        return utils.getInfo().getAllAddresses();
    }

    /**
     * 将cidr格式的IP地址转换为数字与范围进行存储
     * @param args
     */
    public static void main(String[] args) {
        //long,最大值
        Set<String> ipSet = FileUtils.readSetFromResourceFile("china_ip/cidr.txt");
        Set<String> saveIp = new HashSet<>();
        for (String ip : ipSet) {
            //解析 cidr
            String[] ips = analysisCidrIp(ip);
            long min = Long.MAX_VALUE;
            long max = Long.MIN_VALUE;
            for (String s : ips) {
                long ipL = ipToLong(s);
                if(ipL>max){
                    max=ipL;
                }
                if(ipL<min){
                    min=ipL;
                }
            }
            if(max==Long.MIN_VALUE && min==Long.MAX_VALUE){
                continue;
            }
            if(max==Long.MIN_VALUE){
                max = min;
            }
            String save =min+","+(max-min);
            saveIp.add(save);
            System.out.println(save);
        }
        FileUtils.saveSetToResourceFile(saveIp,"china_ip/ip_long.txt");
    }
}
