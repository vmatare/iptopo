package iptopo.crawler;


import iptopo.db.DbClient;
import iptopo.geoip.GeoIPclient;
import iptopo.graph.IpNode;
import iptopo.tracer.Tracer;
import iptopo.tracer.TracerFactory;
import iptopo.tracer.TracerSettings;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class IpCrawler implements Runnable {
	
	private int max_concurrency = 8;
	protected Map<String, Set<Tracer>> running_tracers;
	private BlockingQueue<InetAddress> targets_todo;
	private BlockingQueue<Tracer> tracers_ready;
	private TracerFactory tracer_factory;
	private IpNode root_node;
	private ThreadPoolExecutor executor;
	private GeoIPclient geoip_client;
	private DbClient db;
	
	
	public IpCrawler(TracerFactory tracer_factory, String src_ip, GeoIPclient geoip_client) throws IOException {
		this.tracer_factory = tracer_factory;
		this.geoip_client = geoip_client;
		executor = new ThreadPoolExecutor(2, max_concurrency, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(max_concurrency * 2));
		running_tracers = new ConcurrentHashMap<String, Set<Tracer>>();
		tracers_ready = new ArrayBlockingQueue<Tracer>(max_concurrency);
		tracer_factory.setReadyQueue(tracers_ready);
		targets_todo = new LinkedBlockingQueue<InetAddress>(100);
		db = new DbClient();
		root_node = db.get_node(src_ip);
		tracer_factory.setRoot(root_node);
	}
	
	
	public IpNode get_root() {
		return root_node;
	}


	private void db_update_geoip() {
		List<IpNode> nodes = db.query("select from IpNode where latitude is null or longitude is null");
		geoip_client.queue_for_update(nodes);
	}
	
	public void run() {
		try {
			// Move db to current ThreadLocal since the constructor may have been called in a different thread
//			ODatabaseRecordThreadLocal.INSTANCE.set(db.getDb().getUnderlying());
			
			db_update_geoip();
						
			System.out.println("Crawler " + toString() + ": Waiting for seeds...");
			Tracer tracer_with_result = null;
			while(!Thread.interrupted()) {
				if (tracer_with_result != null) {
					process_results(tracer_with_result);
					if (tracer_with_result.isDone()) {
						String ip = tracer_with_result.getSettings().getTarget().getHostAddress();
						IpNode done_target = db.get_node(ip);
						running_tracers.remove(ip);
						root_node.getDidTrace().add(done_target);
						db.save_node(root_node);
					}
				}
				else if (executor.getActiveCount() < max_concurrency) {
					InetAddress target = targets_todo.take();
					if (target != null) {
						System.out.println("Crawler " + toString() + ": Tracing " + target.getHostAddress() + ".");
						Tracer tracer = tracer_factory.getTracer(new TracerSettings(target));
						Set<Tracer> tracers;
						if (running_tracers.containsKey(target.getHostAddress())) {
							tracers = running_tracers.get(target.getHostAddress());
						}
						else {
							tracers = new HashSet<Tracer>();
							running_tracers.put(target.getHostAddress(), tracers);
						}
						tracers.add(tracer);
						executor.execute(tracer);
					}
				}
				tracer_with_result = tracers_ready.poll(1, TimeUnit.SECONDS);
			}
		} catch (InterruptedException e) {
			System.out.println("Crawler " + toString() + " was interrupted: Shutting down!");
			executor.shutdownNow();
		} catch (IOException e) {
			executor.shutdownNow();
		}
	}
	
	
	private void process_results(Tracer tracer_with_result) throws InterruptedException {
		Tracer.Hop new_hop;
		while ((new_hop = tracer_with_result.getNextHop()) != null) {
			try {
				IpNode from_node = db.get_node(new_hop.from);
				IpNode to_node = db.get_node(new_hop.to);
				from_node.addOutPeer(to_node);
				to_node.addInPeer(from_node);
				db.save_node(to_node, false);
				db.save_node(from_node, false);

				if (from_node.getLatitude() == null || from_node.getLongitude() == null) {
					geoip_client.queue_for_update(from_node); // async db update
				}
				if (to_node.getLatitude() == null || to_node.getLongitude() == null) {
					geoip_client.queue_for_update(to_node); // async db update
				}
			}
			catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
	}
		
	public synchronized void addTarget(InetAddress target) throws InterruptedException {
		if (!running_tracers.containsKey(target)) {
			targets_todo.put(target);
		}
	}
}