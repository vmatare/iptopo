package iptopo;

import iptopo.crawler.IpCrawler;
import iptopo.geoip.GeoIPclient;
import iptopo.seeders.IpListFileSeeder;
import iptopo.tracer.Ipv4TracerFactory;

import java.io.IOException;

public class IpTopo {

	Thread seederThread, crawlerThread, geoipThread;

	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.err.println("Usage: java IpTopo SRC_IP INFILE");
		}
		else {
			IpTopo ipTopo = new IpTopo(args[0], args[1]);
			ipTopo.run();
		}
	}
	
	public void run() {
		geoipThread.start();
		crawlerThread.start();
		seederThread.start();
		try {
			crawlerThread.join();
		} catch (InterruptedException e) {
			seederThread.interrupt();
			crawlerThread.interrupt();
		}
		if (seederThread.isAlive() && !seederThread.isInterrupted()) {
			seederThread.interrupt();
		}
		if (crawlerThread.isAlive() && !crawlerThread.isInterrupted()) {
			crawlerThread.interrupt();
		}
	}
	
	public IpTopo(String src_ip, String seedFile) throws IOException {
		GeoIPclient geoip = new GeoIPclient();
		IpCrawler crawler = new IpCrawler(new Ipv4TracerFactory(), src_ip, geoip);
		crawlerThread = new Thread(crawler, "Ipv4Crawler");
		seederThread = new Thread(new IpListFileSeeder(seedFile, crawler), "Ipv4FileSeeder");
		geoipThread = new Thread(geoip, "GeoIPclient");
	}	
}
