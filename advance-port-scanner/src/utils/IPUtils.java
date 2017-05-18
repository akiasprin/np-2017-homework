package utils;

public class IPUtils {
	public static long str2int(String str) {
		int n = 0;
		try {
			n = Integer.parseInt(str);
		} catch (Exception e) {
		}
		return n;
	}

	public static long stringIpToLong(String ip) {
		String[] splitted = ip.split("\\.");
		return (str2int(splitted[0]) << 24) | (str2int(splitted[1]) << 16) | (str2int(splitted[2]) << 8) | str2int(splitted[3]);
	}

	public static String longIpToString(long ip) {
		return (ip >> 24) + "." + ((ip >> 16) & 255) + "." + ((ip >> 8) & 255) + "." + (ip & 255);
	}
}

