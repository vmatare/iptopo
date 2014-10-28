package iptopo.tracer;

import java.net.UnknownHostException;


public class Ipv4TracerFactory extends TracerFactory {

	public Tracer constructTracer(TracerSettings settings) throws UnknownHostException, RuntimeException {
		Ipv4Traceroute rv = new Ipv4Traceroute(root.getAddr(), settings);
		return rv;
	}
}
