# china-ip-filter
中国大陆IP过滤器-Java

IP文件路径：  
项目的resources/china_ip目录下  

使用ChinaIPRecognizer类即可  

# 更新记录  
201911月30日更新  


原文链接：
[https://copyfuture.com/blogs-details/201912080042520285sdvdbajzqwolso](https://copyfuture.com/blogs-details/201912080042520285sdvdbajzqwolso)

# 概述
本篇讲解如何快速判断IPV4地址是否在大陆境内的IP地址。  

中国IPV4的地址现在大约是3亿4千万个。  

github仓库地址，源码和ip地址都在里面。  
[https://github.com/chenhaoxiang/china-ip-filter](https://github.com/chenhaoxiang/china-ip-filter)

目前最新是2019年11月30日，境内所有ip范围。  

后面会持续更新和维护    

欢迎大伙star    


# 方法一
最简单方法，消耗大量内存，土豪方法。  

在内存中将3亿4千万IP全部存储到Set中。  

如果按照32个字节一个IP来算，大约需要10G左右。  

这种方法就不进行介绍了。  

# 方法二
将IP进行拆分为4段。a、b、c、d段   

分段进行匹配，相较于方法一可以节省3/4的空间，但是需要的内存还是很大。  

按照树形结构进行存储，a匹配才进行b的匹配，b匹配再进行c的匹配，依次匹配  

只有abcd段完全匹配才说明IP在集合中  

# 方法三
通过CIDR格式表示的IPV4地址范围进行处理  

CIDR及地址块计算
IP地址表示法：
IP地址 ::= {<网络前缀>，<主机号>}/网络前缀所占位数 (斜线表示法)

CIDR表示法给出任何一个IP地址就相当于给出了一个CIDR地址块，实现了路由的聚合  
  
使用该方法，CIDR格式表示的IPV4地址块只有几万，存储完全是够的。  

通过存储CIDR的IPV4开始与结束，再将实际的IP进行比较是否在CIDR的块中，即可判断出IP是否在IP集合中  

详情不进行讲解了，个人觉得使用这种方式较为麻烦，因为有着更加简便的方法    

# 方法四  
是我目前所知最好的一种判断IP是否在某些IP范围内的最好方式，如果您有更好方式，欢迎评论  

我们可以知道，通过32位字节便可以存储一个IP地址，32位字节可以转换为long数字，按照方法三的思路，如果使用范围的方式查找ip地址，那么需要的存储空间是非常小的，且效率也非常高。  

那么，可以将CIDR表示的IP地址块通过存储起始IP数字以及其后的范围数字即可（或者存储起始数字和结束数字）  

## 方法四核心类：
```java
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

```











