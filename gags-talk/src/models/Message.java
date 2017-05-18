package models;

import handlers.GagTalk;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Message implements Serializable {

    private Date date;
    private String name;
    private String content;

    public Message(String content) {
        this.content = content;
        this.name = Member.localhost.getName();
        this.date = new Date();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getDate() {
        return date;
    }
    public static String getFormatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        sdf.applyPattern("yyyy/MM/dd HH:mm:ss");
        return sdf.format(date);
    }

    public static Message byte2Msg(byte[] buffer) throws Exception {
        Object ob = null;
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(buffer));
        ob = ois.readObject();
        ois.close();
        return (Message)ob;
    }

    public byte[] msg2Byte() throws Exception {
        byte[] bytes = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(this);
        oos.close();
        bytes = baos.toByteArray();
        baos.close();
        return bytes;
    }
}
