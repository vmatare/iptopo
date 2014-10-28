package iptopo.tracer;

import java.net.InetAddress;

public class TracerSettings {
	public enum IpProto { ICMP, TCP, UDP };
	private IpProto proto;
	private Integer port;
	private InetAddress target;
	private InetAddress root;
	private int startTTL, maxTTL;
	
	public TracerSettings(IpProto proto, int port,
			InetAddress target, InetAddress root, int startTTL, int maxTTL) {
		this.proto = proto;
		this.port = port;
		this.target = target;
		this.root = root;
		this.startTTL = startTTL;
		this.maxTTL = maxTTL;
	}
	public TracerSettings(IpProto proto, InetAddress target) {
		this(proto, 0, target, null, 1, 30);
	}
	public TracerSettings(IpProto proto, InetAddress target, int maxTTL) {
		this(proto, 0, target, null, 1, maxTTL);
	}
	public TracerSettings(IpProto proto, InetAddress target, int startTTL, int maxTTL) {
		this(proto, 0, target, null, startTTL, maxTTL);
	}
	public TracerSettings(InetAddress target, int startTTL, int maxTTL) {
		this(IpProto.UDP, 0, target, null, startTTL, maxTTL);
	}
	public TracerSettings(InetAddress target, int startTTL) {
		this(IpProto.UDP, 0, target, null, startTTL, 30);
	}
	public TracerSettings(InetAddress target) {
		this(IpProto.UDP, 0, target, null, 1, 30);
	}
	
	public InetAddress getTarget() { return target; }
	public InetAddress getRoot() { return root; }
	public int getStartTTL() { return startTTL; }
	public int getMaxTTL() { return maxTTL; };
	public void setTarget(InetAddress target) { this.target = target; };
	public void setStartTTL(int startTTL) { this.startTTL = startTTL; };
	public void setMaxTTL(int m) { this.maxTTL = m; };
	public String toString() {
		return "{target=" + target + ", root=" + root
			+ ", startTTL=" + startTTL + "proto=" + proto + ", port=" + port + " }";
	}
	public IpProto getProto() { return proto; }
	public int getPort() { return port; }
}
