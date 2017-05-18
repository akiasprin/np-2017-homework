package handlers;

import utils.IPUtils;
import view.MainGUI;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

class ScheduleICMPScanTask implements Callable<Void> {

    private InetAddress address;

    public ScheduleICMPScanTask(String address) {
        try {
            this.address = InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public Void call() throws Exception {
        ICMPScanner.scanned.incrementAndGet();
        if (address.isReachable(3000)) {
            ICMPScanner.lock.lock();

            MainGUI.ICMPTab.addResult(address.getHostAddress());
            MainGUI.ICMPTab.setStatus("(" + ICMPScanner.scanned.get() + "/" + ICMPScanner.count + ")" +
                    "[" + address.getHostAddress() + "]: Success");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    MainGUI.ICMPTab.setHostname(address.getHostAddress(), address.getCanonicalHostName());
                }
            }).start();

            new Thread(new Runnable() {

                String result = "";
                String[] cmd = {};
                Process process;

                private String exec() throws Exception {
                    process = Runtime.getRuntime().exec(cmd);
                    return new BufferedReader(
                            new InputStreamReader(process.getInputStream())).readLine();
                }

                @Override
                public void run() {
                    try {
                        if (System.getProperty("os.name").equals("Linux")) {
                            cmd = new String[]{"/bin/sh", "-c",
                                        "arp -n | grep \"^" + address.getHostAddress() + "\\s\" | awk '{print $3}'"};
                            result = exec();
                            if (null == result || "".equals(result)) {
                                cmd = new String[]{"/bin/sh", "-c",
                                        "ifconfig | grep -C 1 " + address.getHostAddress() +
                                                " | grep HWaddr | awk '{print $5}'"};
                                result = exec();
                            }
                        } else {
                            result = "不支持Win系统";
                        }
                        MainGUI.ICMPTab.setMAC(address.getHostAddress(), result);
                        //process.waitFor();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }).start();
            ICMPScanner.lock.unlock();
            //ICMPScanner.live_hosts.add(address.getHostAddress());

        } else {
            ICMPScanner.lock.lock();
            MainGUI.ICMPTab.setStatus("(" + ICMPScanner.scanned.get() + "/" + ICMPScanner.count + ")" +
                    "[" + address.getHostAddress() + "]: Failure");
            ICMPScanner.lock.unlock();
        }
        checkFinish();
        return null;
    }

    private void checkFinish() {
        if (ICMPScanner.scanned.get() == ICMPScanner.count) {
            MainGUI.ICMPTab.setTasksFinish();
        }
    }
}


public class ICMPScanner {

    public static Vector<String> live_hosts; //not safe
    public static ReentrantLock lock;
    public static AtomicBoolean isSuspended;
    public static AtomicInteger scanned;
    public static ExecutorService pool;
    public static long count;

    public static void suspendService() {
        if (isSuspended.get()) {
            synchronized (isSuspended) {
                isSuspended.notify();
            }
        }
        isSuspended.set(!isSuspended.get());
    }

    public static void stopAndCleanService() {
        try {
            pool.shutdownNow();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void createTask(String ip_start_str, String ip_stop_str) {

        long ip_start = IPUtils.stringIpToLong(ip_start_str);
        long ip_stop = IPUtils.stringIpToLong(ip_stop_str);

        lock = new ReentrantLock();
        count = ip_stop - ip_start + 1;
        scanned = new AtomicInteger(0);
        isSuspended = new AtomicBoolean(false);

        long sleepInterval = Math.max(256 * 1024 / 74 / 1000 / 2, 1);

        long running = 0;
        try {
            pool = Executors.newFixedThreadPool(100);
            for (long ip = ip_start; ip <= ip_stop; ip++) {
                synchronized (isSuspended) {
                    if (isSuspended.get()) {
                        isSuspended.wait();
                    }
                }
                MainGUI.ICMPTab.setProgressBar((int) (100 * ++running / count));
                pool.submit(new ScheduleICMPScanTask(IPUtils.longIpToString(ip)));
                if (++running % sleepInterval == 0) {
                    Thread.sleep(1, 120);
                }
            }

            MainGUI.ICMPTab.setDistrbutedFinish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}