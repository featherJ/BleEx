package com.bleex.consts;

/**
 * 数据标签
 *
 * @author Agua.L
 */
public class DataTags {
    /**
     * M->S的长数据写的标签
     */
    public static byte[] MS_WRITE_LARGE = new byte[]{120, 110};
    /**
     * S->M的长数据写的标签
     */
    public static byte[] SM_WRITE_LARGE = new byte[]{110, 100};
    /**
     * M->S的长数据请求的标签
     */
    public static byte[] MS_REQUEST_LARGE = new byte[]{88, 99};
    /**
     * S->M的长数据请求应答的标签
     */
    public static byte[] SM_RESPONSE_LARGE = new byte[]{99, 88};
}
