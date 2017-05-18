package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Vector;

public class FileUtils {
    public static class Reader {
        public static String read(String filename) {
            String result = "";
            try {
                FileInputStream fis = new FileInputStream(filename);
                FileChannel channel = fis.getChannel();
                long size = channel.size();
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                int len;
                byte[] bytes = new byte[1024];
                while ((len = channel.read(byteBuffer)) != -1) {
                    // 注意先调用flip方法反转Buffer,再从Buffer读取数据
                    byteBuffer.flip();
                    result += new String(byteBuffer.array(), 0, len);
                    // 最后注意调用clear方法,将Buffer的位置回归到0
                    byteBuffer.clear();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }
    }

    public static void main(String args[]) {
        String result;
        result = Reader.read("dict.txt");
        System.out.println(result.split("\n").length);
    }
}
