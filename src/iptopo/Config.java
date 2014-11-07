package iptopo;

import java.util.ResourceBundle;

public class Config {
	public final static String DEFAULT_SRC = "/etc/iptopo.conf";
	private static boolean aggressiveTracing = false;
	private static final ResourceBundle properties = ResourceBundle.getBundle("iptopo");

	public static boolean getAggressiveTracing() {
		return aggressiveTracing;
	}

	public static String getDbName() {
		return properties.getString("db.name");
	}
	public static String getDbUser() {
		return properties.getString("db.user");
	}
	public static String getDbPassword() {
		return properties.getString("db.password");
	}
	public static String getDbLocalPath() {
		return properties.getString("db.localpath");
	}
}
