package iptopo.tracer;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @class Tracer
 * A Runnable that traces hops to a target and queues them up for consumption by another thread.
 * Users need to setReadyQueue() before running this.
 */
public abstract class Tracer implements Runnable {
	
	private TracerSettings settings;
	private BlockingQueue<Tracer> tracersReady = null;
	private BlockingQueue<Hop> results;
	private Hop lastHop = null;
	private Date lastUpdate;
	private final String root;
	private boolean tracingDone;
	
	public Tracer(String root, TracerSettings settings) throws UnknownHostException {
		this.root = root;
		this.settings = settings;
		results = new LinkedBlockingQueue<Hop>();
		tracingDone = false;
	}
	public TracerSettings getSettings() { return settings; }
	public Hop getNextHop() { return results.poll(); }
	
	public void setReadyQueue(BlockingQueue<Tracer> tracersReady) {
		this.tracersReady = tracersReady;
	}
	
	public abstract void trace();
	public void run() {
		trace();
		setTracingDone(true);
	}
	public synchronized boolean isDone() { return tracingDone && results.isEmpty(); };
	public synchronized void setTracingDone(boolean b) { tracingDone = b; };
		
	protected void addHop(String addr) throws InterruptedException {
		try {
			InetAddress parsedAddr = InetAddress.getByName(addr);

			// ignore local and unknown addresses
			if (parsedAddr.isAnyLocalAddress()
					|| parsedAddr.isMulticastAddress()
					|| parsedAddr.isSiteLocalAddress()
					|| parsedAddr.isLinkLocalAddress()) return;

			lastUpdate = new Date();

			if (lastHop == null) {
				lastHop = new Hop(root, addr, lastUpdate); 
			}
			else {
				lastHop = new Hop(lastHop.to, addr, lastUpdate);
			}
			
			results.add(lastHop);
			tracersReady.put(this);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Date getLastUpdate() { return lastUpdate; }
	
	public class Hop {
		public String from, to;
		public Date timestamp;
		public Hop(String from, String to, Date timestamp) {
			this.from = from;
			this.to = to;
			this.timestamp = timestamp;
		}
	}
}
