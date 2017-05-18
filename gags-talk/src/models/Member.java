package models;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class Member implements Serializable {

    private String name;
    private InetSocketAddress socketAddress;

    public static Member localhost = new Member();
    public static Member multicast = new Member();

    public Member() {
    }

    public Member(String name, InetSocketAddress socketAddress) {
        this.name = name;
        this.socketAddress = socketAddress;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public InetSocketAddress getSocketAddress() {
        return socketAddress;
    }

    public void setSocketAddress(InetSocketAddress socketAddress) {
        this.socketAddress = socketAddress;
    }
}