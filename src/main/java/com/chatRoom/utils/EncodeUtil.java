package com.chatRoom.utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @Author Jinquan_Ou
 * @Description 使用MD5加密的工具类
 * @Date 2023-06-01 0:10
 * @Version 1.0.0
 **/
public class EncodeUtil {

    /**
     * 编码方法
     * @param str 要编码的字符
     * @return 编码后的结果
     */
    public static String encrypt(String str) {
        try {
            // 创建MD5散列函数实例
            MessageDigest md = MessageDigest.getInstance("MD5");

            // 将密码转换为字节数组并进行散列计算
            byte[] messageDigest = md.digest(str.getBytes());

            // 将字节数组转换为十六进制字符串
            BigInteger no = new BigInteger(1, messageDigest);
            String hashText = no.toString(16);

            // 如果十六进制字符串长度不足32位，在前面补0
            while (hashText.length() < 32) {
                hashText = "0" + hashText;
            }

            return hashText;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 判断是否符合
     * @param source 源字符串
     * @param object 比较对象字符串
     * @return 是否相符
     */
    public static boolean pattern(String source, String object){
        if(encrypt(source).equals(object)){
            return true;
        }else {
            return false;
        }
    }
}
