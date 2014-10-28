package iptopo;

public class Config {
	public final static String DEFAULT_SRC = "/etc/iptopo.conf";
	private static boolean aggressiveTracing = false;
	
	public static boolean getAggressiveTracing() {
		return aggressiveTracing;
	}
}
