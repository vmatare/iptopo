package iptopo.seeders;

import iptopo.DbClient;
import iptopo.crawler.IpCrawler;
import iptopo.graph.IpNode;

import java.io.IOException;
import java.net.InetAddress;

import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;

public abstract class IpSeeder implements Runnable {
	private IpCrawler crawler;
	private DbClient db;

	public IpSeeder(IpCrawler crawler) throws IOException {
		this.crawler = crawler;
		db = new DbClient();
	}
	
	protected abstract void seed();
	
	public void run() {
		ODatabaseRecordThreadLocal.INSTANCE.set(db.getDb().getUnderlying());
		seed();
	}
	
	protected void add(InetAddress addr) throws InterruptedException {
		IpNode root = db.getDb().load(crawler.get_root());
		db.getDb().detach(root);
		if (!root.hasTraced(addr)) {
			crawler.addTarget(addr);
		}
	}
}
