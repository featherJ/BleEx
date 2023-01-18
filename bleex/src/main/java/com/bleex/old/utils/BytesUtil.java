package com.bleex.old.utils;

/**
 * 二进制工具
 *
 * @author Agua.L
 */
public class BytesUtil {
    /**
     * 字节转字符串
     *
     * @param b
     * @return
     */
    public static String byteToString(byte b, boolean hex) {
        String str = hex ? Integer.toHexString(b & 0xFF) : Integer.toString(b & 0xFF);
        //由于十六进制是由0~9、A~F来表示1~16，所以如果Byte转换成Hex后如果是<16,就会是一个字符（比如A=10），通常是使用两个字符来表示16进制位的,
        //假如一个字符的话，遇到字符串11，这到底是1个字节，还是1和1两个字节，容易混淆，如果是补0，那么1和1补充后就是0101，11就表示纯粹的11
        if (str.length() < 2 && hex) {
            str = new StringBuilder(String.valueOf(0)).append(str).toString();
        }
        return str.toUpperCase();
    }

    /**
     * 字节数组转字符串
     *
     * @param bytes
     * @return
     */
    public static String bytesToString(byte[] bytes, boolean hex) {
        StringBuffer sb = new StringBuffer();
        if (bytes != null && bytes.length > 0) {
            for (int i = 0; i < bytes.length; i++) {
                String str = byteToString(bytes[i], hex);
                sb.append(str);
                if (i != bytes.length - 1) {
                    sb.append(",");
                }
            }
        }
        return "[" + sb.toString() + "] length:" + bytes.length;
    }

    public static boolean equals(byte[] a, byte[] b) {
        return bytesToString(a,true).equals(bytesToString(b,true));
    }
}
