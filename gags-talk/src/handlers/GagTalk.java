package handlers;

import view.GagTalkGUI;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.*;
import models.*;



public class GagTalk {

    private InetAddress group;
    private int groupPort = 9527;
    private DatagramChannel channel;
    private HashMap<String, Member> memberMap;
    private Vector<Member> memberList;
    public Boolean isStop = false;

    public GagTalk(String nickname, String networkInterfaceName) {
        try {
            memberMap = new HashMap<>();
            memberList = new Vector<>();

            Member.localhost.setName(nickname);
            Member.localhost.setSocketAddress(new InetSocketAddress(groupPort));

            group = InetAddress.getByName("224.1.1.108");
            Member.multicast.setSocketAddress(new InetSocketAddress(group, groupPort));

            channel = DatagramChannel.open(StandardProtocolFamily.INET);
            //channel.configureBlocking(false);
            channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            channel.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, true);
            DatagramChannel bind = channel.bind(Member.localhost.getSocketAddress());
            NetworkInterface networkInterface = NetworkInterface.getByName(networkInterfaceName);
            channel.join(group, networkInterface);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class Postman implements Runnable {

        private Message message;
        private Member target;

        public Postman(Message message, Member target) {
            this.message = message;
            this.target = target;
        }

        public void run() {
            try {
                channel.send(ByteBuffer.wrap(message.msg2Byte()), target.getSocketAddress());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class Receiver implements Runnable {

        private Message message;
        private Member member;
        private InetSocketAddress source = null;

        private class Resolver {

            public Resolver() throws UnknownHostException {
                String[] data = message.getContent().split(" ", 2);


                if ("NICE".equalsIgnoreCase(data[0])) { //某人上线
                    member = new Member(message.getName(), source);

                    GagTalkGUI.self.setContent(message.getDate(), "系统消息",
                            "GAG王" + message.getName() + "已登录, 来自"
                                    + source.getHostName() + "/" + source.getAddress().getHostAddress());

                    GagTalkGUI.self.addMember(member);

                    memberMap.put(message.getName(), member); //添加成员索引
                    memberList.add(member); //添加成员列表
                    new Thread(new Postman(new Message("MAMA " + Member.localhost.getName()),
                            member)).start(); //告知某人该主机存活
                } else if ("SUCK".equalsIgnoreCase(data[0])) { //某人下线
                    member = new Member(message.getName(), source);

                    GagTalkGUI.self.setContent(message.getDate(), "系统消息",
                            "GAG王" + message.getName() + "已下线, 来自"
                                    + source.getHostName() + "/" + source.getAddress().getHostAddress());
                    GagTalkGUI.self.removeMember(member);

                    memberMap.remove(message.getName());
                    memberList.remove(new Member(message.getName(), source));

                } else if ("MAMA".equalsIgnoreCase(data[0])) { //存活主机更新
                    if (!Member.localhost.getName().equals(message.getName())) { //非本机
                        member = new Member(message.getName(), source);

                        GagTalkGUI.self.setContent(message.getDate(), "系统消息",
                                "GAG王" + message.getName() + "在线, 来自"
                                        + source.getHostName() + "/" + source.getAddress().getHostAddress());
                        GagTalkGUI.self.addMember(member);

                        memberMap.put(message.getName(), member); //添加成员索引
                        memberList.add(new Member(message.getName(), source)); //添加成员列表
                    }
                } else { //普通内容
                    if (!Member.localhost.getName().equals(message.getName())) { //非本机
                        GagTalkGUI.self.setContent(message.getDate(), message.getName(), data[1]);
                    }
                }

            }
        }

        public void run() {
            ByteBuffer buffer = ByteBuffer.allocate(8192);
            while (!isStop) {
                try {
                    if ((source = (InetSocketAddress) channel.receive(buffer)) != null) {
                        buffer.flip();
                        message = Message.byte2Msg(buffer.array());
                        new Resolver();
                        buffer.clear();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void loginGagKing() {

        new Thread(new Postman(
                new Message("NICE"), Member.multicast
        )).start();
    }

    public void logoffGagKing() {
        new Thread(new Postman(
                new Message("SUCK"), Member.multicast
        )).start();
    }

    public void postMulticastGag(String gag) {
        Message message = new Message("AGAG " + gag);

        GagTalkGUI.self.setContent(message.getDate(), message.getName(), message.getContent().substring(5));

        new Thread(new Postman(
                message, Member.multicast
        )).start();

    }

    public void postPersonalGag(String gag, String to) {
        Message message = new Message("AGAG " + gag);
        GagTalkGUI.self.setContent(message.getDate(), message.getName(), message.getContent().substring(5));

        Member member = memberMap.get(to);
        if (member != null) {
            new Thread(new Postman(
                    message, member
            )).start();
        } else {
            new Thread(new Postman(
                    message, Member.multicast
            )).start();

        }
    }

    public void get() {
        new Thread(new Receiver()).start();
    }
}
