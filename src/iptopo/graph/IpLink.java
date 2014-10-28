package iptopo.graph;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class IpLink {
	private IpNode source;
	private IpNode target;
	private Set<Date> observations;
	
	public Set<Date> getObservations() {
		return observations;
	}

	public void setObservations(Set<Date> observations) {
		this.observations = observations;
	}

	public IpLink() { this(null, null, new Date()); }
	
	public IpLink(IpNode from, IpNode to, Date time) {
		this.setSource(from);
		this.setTarget(to);
		this.observations = new HashSet<Date>();
		observations.add(time);
	}
	
	public void addObservation(Date time) {
		observations.add(time);
	}
	
	public IpNode getSource() {
		return source;
	}

	public void setSource(IpNode source) {
		this.source = source;
	}

	public IpNode getTarget() {
		return target;
	}

	public void setTarget(IpNode target) {
		this.target = target;
	}
	
	public String toString() {
		return source + " -> " + target;
	}
}
