package iptopo.geoip;

import iptopo.db.DbClient;
import iptopo.graph.IpNode;

import java.io.IOException;
import java.io.StringReader;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;


public class GeoIPclient implements Runnable {
	private static final String geoip_service = "http://freegeoip.net/json";
	
	private BlockingQueue<IpNode> jobs;
	private WebTarget rest_target;
	private DbClient db;
	
	public GeoIPclient() {
		jobs = new LinkedBlockingQueue<IpNode>();
		rest_target = ClientBuilder.newClient().target(geoip_service);
	}
	
	public void queue_for_update(IpNode node) {
		jobs.add(node);
	}
	
	public void queue_for_update(Collection<IpNode> nodes) {
		jobs.addAll(nodes);
	}
	
	
	public void run() {
		IpNode node;
		try {
			db = new DbClient();
			while (!Thread.interrupted()) {
				node = jobs.take();
				String json_str = rest_target.path(node.getAddr()).request().get(String.class);
				JsonObject json_obj = Json.createReader(new StringReader(json_str)).readObject();
				node.setLatitude(json_obj.getJsonNumber("latitude").doubleValue());
				node.setLongitude(json_obj.getJsonNumber("longitude").doubleValue());
				db.save_node(node, true);

			}
		}
		catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		catch (InterruptedException e) {}
		finally {
			db.close();
		}
	}
}
