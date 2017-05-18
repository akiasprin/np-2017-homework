package services;

import java.util.HashMap;
import java.util.Map;

public class PortScannerPolicies {

	private static PortScannerPolicies instance;

	public Map db;

	PortScannerPolicies() {
		db = new HashMap<Integer, String[]>();
		db.put(21, new String[]{ "SYST\n", ".*" });
		db.put(80, new String[]{ "HEAD / HTTP/1.0 \n\n", "^Server: .*$" });
		db.put(88, new String[]{ "HEAD / HTTP/1.0 \n\n", "^Server: .*$" });
		db.put(443, new String[]{ "HEAD / HTTP/1.0 \n\n", "^Server: .*$" });
		db.put(8080, new String[]{ "HEAD / HTTP/1.0 \n\n", "^Server: .*$" });
	}

	public static PortScannerPolicies getInstance() {

		if (instance == null)
			instance = new PortScannerPolicies();
		return instance;
	}

	public static void main(String[] args) {
		PortScannerPolicies.getInstance();
	}
}
