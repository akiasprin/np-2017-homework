package handlers;

import services.PortScannerPolicies;
import utils.IPUtils;
import view.MainGUI;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

class Destination {
	private String ip;
	private int port;

	Destination(String ip, int port) {
		this.ip = ip;
		this.port = port;
	}

	public String getIP() {
		return ip;
	}

	public int getPort() {
		return port;
	}
}



class SchedulePortScanTask implements Callable<Void> {
	private Destination destination;
	private AsynchronousSocketChannel socketChannel;

	SchedulePortScanTask(Destination destination) {
		this.destination = destination;
	}

	public Void call() throws Exception {
		socketChannel = AsynchronousSocketChannel.open(TCPScanner.channelGroup);
		socketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 128 * 1024);
		socketChannel.setOption(StandardSocketOptions.SO_SNDBUF, 128 * 1024);
		socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, false);
		socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);

		socketChannel.connect(new InetSocketAddress(destination.getIP(), destination.getPort()),
				socketChannel, solve());
		return null;
	}

	private CompletionHandler solve() {
		return new CompletionHandler<Void, AsynchronousSocketChannel>() {

			public void completed(Void result, AsynchronousSocketChannel socketChannel) {
				TCPScanner.scanned.incrementAndGet();

				TCPScanner.lock.lock();
				MainGUI.TCPTab.addResult(destination.getIP(), destination.getPort());
				MainGUI.TCPTab.setStatus("(" + TCPScanner.scanned.get() + "/" + TCPScanner.count + ")" +
						"[" + destination.getIP() + ":" + destination.getPort() + "]: " +
						"Success");
				TCPScanner.lock.unlock();

				//基于规则读取进行试探
				//Bug: 1.FTP不能跳过读取欢迎报文
				String[] policy = (String[]) PortScannerPolicies.getInstance().
						db.get(destination.getPort());
				if (policy == null) {
					ByteBuffer dst = ByteBuffer.allocate(65535);
					socketChannel.read(dst, 10, TimeUnit.SECONDS, dst, ReaderHandler(dst));
				} else {
					ByteBuffer src = String2BB(policy[0]);
					socketChannel.write(src, 10, TimeUnit.SECONDS, src, WriterHandler(src));
				}
			}

			public void failed(Throwable exc, AsynchronousSocketChannel socketChannel) {
				TCPScanner.scanned.incrementAndGet();

				TCPScanner.lock.lock();
				MainGUI.TCPTab.setStatus("(" + TCPScanner.scanned.get() + "/" + TCPScanner.count + ")" +
						"[" + destination.getIP() + ":" + destination.getPort() + "]: " +
						exc.getMessage());
				TCPScanner.lock.unlock();

				checkFinish();
			}

			//报文读取
			private CompletionHandler ReaderHandler(ByteBuffer dst) {
				return new CompletionHandler<Integer, ByteBuffer>() {
					public void completed(Integer ret, ByteBuffer dst) { //ret: -1 or >0

						TCPScanner.lock.lock();
						String resp = "";
						if (ret > 0)
							resp = BB2String(ret, dst);
						MainGUI.TCPTab.setResp(destination.getIP(), destination.getPort(), resp);
						TCPScanner.lock.unlock();

						checkFinish();
					}

					public void failed(Throwable exc, ByteBuffer byteBuffer) {
						//System.out.println("Reading Failed or received 0 byte.");

						checkFinish();
					}
				};
			}

			//报文发送
			private CompletionHandler WriterHandler(ByteBuffer src) {
				return new CompletionHandler<Integer, ByteBuffer>() {
					public void completed(Integer ret, ByteBuffer src) {
						ByteBuffer dst = ByteBuffer.allocate(65535);
						socketChannel.read(dst, 10, TimeUnit.SECONDS, dst, ReaderHandler(dst));
					}

					public void failed(Throwable exc, ByteBuffer byteBuffer) {
						exc.printStackTrace();
					}
				};
			}

			private void checkFinish() {
				try {
					socketChannel.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (TCPScanner.scanned.get() == TCPScanner.count) {
					TCPScanner.stopAndCleanService();
					MainGUI.TCPTab.setTasksFinish();
				}
			}

			private String BB2String(int len, ByteBuffer byteBuffer) {
				byte[] bytes = new byte[65535];
				byteBuffer.flip();
				byteBuffer.get(bytes, 0, len);
				byteBuffer.clear();
				return new String(bytes, 0, len);
			}

			private ByteBuffer String2BB(String str) {
				return ByteBuffer.wrap(str.getBytes());
			}
		};
	}


}

public class TCPScanner {
	public static long count;
	public static ReentrantLock lock;
	public static AtomicInteger scanned;
	public static AtomicBoolean isSuspended;
	public static ExecutorService pool;
	public static AsynchronousChannelGroup channelGroup;

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
			channelGroup.shutdownNow();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void createTask(String ip_start_str, String ip_stop_str,
	                              int port_start, int port_stop) {

		long ip_start = IPUtils.stringIpToLong(ip_start_str);
		long ip_stop = IPUtils.stringIpToLong(ip_stop_str);

		count = ((ip_stop - ip_start) + 1) * ((port_stop - port_start) + 1);
		lock = new ReentrantLock();
		scanned = new AtomicInteger(0);
		isSuspended = new AtomicBoolean(false);

		try {

			pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
			channelGroup = AsynchronousChannelGroup.withThreadPool(pool);

			long running = 0;
			// 74byte per Packet
			long sleepInterval = Math.max(256 * 1024 / 74 / 1000 / 2, 1);

			while (ip_start <= ip_stop) {
				int port = port_start;
				while (port <= port_stop) {
					synchronized (isSuspended) {
						if (isSuspended.get()) {
							isSuspended.wait();
						}
					}
					pool.submit(new SchedulePortScanTask(new Destination(IPUtils.longIpToString(ip_start), port++)));
					if (++running % sleepInterval == 0) {
						Thread.sleep(1, 120);
					}
					MainGUI.TCPTab.setProgressBar((int) (100 * running / count));
				}
				ip_start++;
			}

			MainGUI.TCPTab.setDistrbutedFinish();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
