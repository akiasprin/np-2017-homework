package handlers;

import utils.IPUtils;
import utils.RemoteShellUtils;
import view.MainGUI;

import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

class ScheduleSSHScanTask implements Callable<Void> {

    private String ip;
    private int port;
    private String username;
    private String password;

    public ScheduleSSHScanTask(String ip, int port, String username, String password) {
        this.ip = ip;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public Void call() throws Exception {
        SSHScanner.scanned.incrementAndGet();

        if (RemoteShellUtils.connect(ip, port, username, password)) {
            SSHScanner.lock.lock();
            MainGUI.SSHTab.addResult(ip, String.valueOf(port), username, password);
            MainGUI.SSHTab.setStatus("(" + SSHScanner.scanned.get() + "/" + SSHScanner.count + ")" +
                    "[" + username + ":" + password + "]: Success");
            SSHScanner.lock.unlock();
            checkFinish(true);

        } else {
            SSHScanner.lock.lock();
            MainGUI.SSHTab.setStatus("(" + SSHScanner.scanned.get() + "/" + SSHScanner.count + ")" +
                    "[" + username + ":" + password + "]: Failure");
            SSHScanner.lock.unlock();
            checkFinish(false);
        }

        return null;
    }

    private void checkFinish(Boolean found) {
        if (SSHScanner.scanned.get() == SSHScanner.count || found) {
            MainGUI.SSHTab.setTasksFinish();
            SSHScanner.stopAndCleanService();
        }
    }
}

public class SSHScanner {

    public static String[] dict;
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

    public static void setDict(String[] _dict) {
        dict = _dict;
        count = _dict.length;
    }

    public static void createTask(String ip, int port, String username) {

        lock = new ReentrantLock();
        scanned = new AtomicInteger(0);
        isSuspended = new AtomicBoolean(false);

        long sleepInterval = Math.max(256 * 1024 / 74 / 1000 / 2, 1);


        long running = 0;
        pool = Executors.newFixedThreadPool(8);
        try {
            for (int i = 0; i < count; i++) {
                synchronized (isSuspended) {
                    if (isSuspended.get()) {
                        isSuspended.wait();
                    }
                }
                MainGUI.SSHTab.setProgressBar((int) (100 * ++running / count));
                pool.submit(new ScheduleSSHScanTask(ip, port, username, dict[i]));
                if (++running % sleepInterval == 0) {
                    Thread.sleep(1, 120);
                }
            }
            MainGUI.SSHTab.setDistrbutedFinish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
