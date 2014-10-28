package iptopo.tracer;

import iptopo.graph.IpNode;

import java.net.UnknownHostException;

import java.util.concurrent.BlockingQueue;

public abstract class TracerFactory {

	protected BlockingQueue<Tracer> tracersReady;
	protected IpNode root;

	public TracerFactory() {}
	
	public IpNode getRoot() {
		return root;
	}

	public void setRoot(IpNode root) {
		this.root = root;
	}

	public void setReadyQueue(BlockingQueue<Tracer> tracersReady) {
		this.tracersReady = tracersReady;
	}

	protected abstract Tracer constructTracer(TracerSettings settings) throws UnknownHostException, RuntimeException;
	
	public Tracer getTracer(TracerSettings settings) throws UnknownHostException, RuntimeException {
		Tracer tracer = constructTracer(settings);
		tracer.setReadyQueue(tracersReady);
		return tracer;
	}
}
