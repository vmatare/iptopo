package iptopo.graph;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.HashSet;
import java.util.Set;

public class IpNode {
		
	private String addr;
	private Set<IpNode> inPeers;
	private Set<IpNode> outPeers;
	private Set<IpNode> tracedBy;
	private Set<IpNode> didTrace;
	private Double longitude, latitude;
			

	private void init() {
		inPeers = new HashSet<IpNode>();
		outPeers = new HashSet<IpNode>();
		tracedBy = new HashSet<IpNode>();
		didTrace = new HashSet<IpNode>();
		latitude = null;
		longitude = null;		
	}
	
	public IpNode(String ipAddr) throws UnknownHostException {
		init();
		InetAddress.getByName(ipAddr);
		this.addr = ipAddr;
	}
	
	public IpNode() {
		init();
	}


	public synchronized Double getLongitude() {
		return longitude;
	}

	public synchronized void setLongitude(Double longitude) {
		this.longitude = longitude;
	}

	public synchronized Double getLatitude() {
		return latitude;
	}

	public synchronized void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public synchronized Set<IpNode> getTracedBy() {
		return tracedBy;
	}

	public synchronized void setTracedBy(Set<IpNode> tracedBy) {
		this.tracedBy = tracedBy;
	}

	public synchronized Set<IpNode> getDidTrace() {
		return didTrace;
	}

	public synchronized void setDidTrace(Set<IpNode> didTrace) {
		this.didTrace = didTrace;
	}
	
	public synchronized boolean hasTraced(InetAddress addr) {
		return hasTraced(addr.getHostAddress());
	}
	
	public synchronized boolean hasTraced(String addr) {
		try {
			return didTrace.contains(new IpNode(addr));
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		}		
	}

	public synchronized String getAddr() {
		return addr;
	}
	
	public synchronized void setAddr(String ipAddr) throws UnknownHostException {
		InetAddress.getByName(ipAddr);
		this.addr = ipAddr;
	}
	
	public synchronized void setInPeers(Set<IpNode> inPeers) {
		this.inPeers = inPeers;
	}
	
	public synchronized void setOutPeers(Set<IpNode> outPeers) {
		this.outPeers = outPeers;
	}

	public synchronized boolean hasOutPeer(IpNode node) {
		if (outPeers != null)
			return outPeers.contains(node);
		return false;
	}
	public synchronized boolean hasInPeer(IpNode node) {
		if (inPeers != null)
			return inPeers.contains(node);
		return false;
	}
	
	public synchronized void addInPeer(IpNode node) {
		getInPeers().add(node);
	}
	public synchronized void addOutPeer(IpNode node) {
		getOutPeers().add(node);
	}
		
	public synchronized boolean equals(Object o) {
		if (o != null && o instanceof IpNode) {
			if (((IpNode)o).getAddr() != null)
				return ((IpNode)o).getAddr().equals(this.getAddr());
		}
		return false;
	}
	
	public synchronized Set<IpNode> getInPeers() { return inPeers; };
	public synchronized Set<IpNode> getOutPeers() { return outPeers; };
	
	public synchronized String toString() {
		return addr.toString()
				+ "\n   -> " + listPeers(outPeers)
				+ "\n   <- " + listPeers(inPeers)
				+ "\n";
	}
	
	private synchronized String listPeers(Set<IpNode> peers) {
		StringBuilder rv = new StringBuilder();
		rv.append("{");
		for (IpNode node : peers) {
			rv.append(node.getAddr());
			rv.append(", ");			
		}
		if (rv.toString().endsWith(", ")) {
			rv.delete(rv.length()-2, rv.length());
		}
		rv.append("}");
		return rv.toString();
	}
	
	public synchronized int hashCode() { return getAddr() == null ? 0 : getAddr().hashCode(); }
	
	public synchronized void merge(IpNode other, boolean update_loc) {
		if (!this.equals(other)) {
			throw new RuntimeException("Refusing to merge node " + other.getAddr() + " into " + getAddr() + ".");
		}
		if (other.getLatitude() != null && (getLatitude() == null || update_loc)) {
			setLatitude(other.getLatitude());
		}
		if (other.getLongitude() != null && (getLongitude() == null || update_loc)) {
			setLongitude(other.getLongitude());
		}
		
		if (other.getDidTrace() != null) getDidTrace().addAll(other.getDidTrace());
		if (other.getInPeers() != null) getInPeers().addAll(other.getInPeers());
		if (other.getOutPeers() != null) getOutPeers().addAll(other.getOutPeers());
		if (other.getTracedBy() != null) getTracedBy().addAll(other.getTracedBy());
	}

}
