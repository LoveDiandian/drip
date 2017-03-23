package com.drip.sftp;

import com.jcraft.jsch.ChannelSftp;

import java.util.HashMap;
import java.util.Map;

public class SFTPTest {

    public SFTPChannel getSFTPChannel() {
        return new SFTPChannel();
    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        SFTPTest test = new SFTPTest();

        Map<String, String> sftpDetails = new HashMap<String, String>();
        // 设置主机ip，端口，用户名，密码
        sftpDetails.put(SFTPConstants.SFTP_REQ_HOST, "10.32.92.124");
        sftpDetails.put(SFTPConstants.SFTP_REQ_USERNAME, "sftp");
        sftpDetails.put(SFTPConstants.SFTP_REQ_PASSWORD, "KyDj1228");
        sftpDetails.put(SFTPConstants.SFTP_REQ_PORT, "22");
        
        String src = "D:\\SourceCounter.zip"; // 本地文件名
        String dst = "/upload/temp/SourceCounter.zip"; // 目标文件名
              
        SFTPChannel channel = test.getSFTPChannel();
        ChannelSftp chSftp = channel.getChannel(sftpDetails, 60000);
        
        /**
         * 代码段1
        OutputStream out = chSftp.put(dst, ChannelSftp.OVERWRITE); // 使用OVERWRITE模式
        byte[] buff = new byte[1024 * 256]; // 设定每次传输的数据块大小为256KB
        int read;
        if (out != null) {
            System.out.println("Start to read input stream");
            InputStream is = new FileInputStream(src);
            do {
                read = is.read(buff, 0, buff.length);
                if (read > 0) {
                    out.write(buff, 0, read);
                }
                out.flush();
            } while (read >= 0);
            System.out.println("input stream read done.");
        }
        **/

        //chSftp.put(src, dst, ChannelSftp.OVERWRITE); // 代码段2

        chSftp.get("/upload/temp/SourceCounter.zip","D:\\SourceCounter_bak.zip");

        // chSftp.put(new FileInputStream(src), dst, ChannelSftp.OVERWRITE); // 代码段3
        chSftp.quit();
        channel.closeChannel();
    }
}