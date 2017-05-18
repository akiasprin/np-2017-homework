package services;

import java.util.HashMap;
import java.util.Map;

public class PortScannerPDD {
	private static PortScannerPDD instance;
	public Map<Integer, String> db;

	PortScannerPDD() {
		db = new HashMap<>();
		db.put(20, "FTP data transfer");
		db.put(21, "FTP control");
		db.put(22, "SSH");
		db.put(25, "SMTP");
		db.put(53, "DNS");
		db.put(69, "TFTP");
		db.put(80, "HTTP");
		db.put(111, "PortMap");
		db.put(443, "HTTPS");
		db.put(530, "RPC");
		db.put(2333, ":)");
		db.put(3306, "MYSQL");
		db.put(3389, "RDP");
		db.put(23333, ":)");
	}

	public static PortScannerPDD getInstance() {
		if (instance == null)
			instance = new PortScannerPDD();
		return instance;
	}

}
