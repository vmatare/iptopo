package iptopo.seeders;

import iptopo.crawler.IpCrawler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class IpListFileSeeder extends IpSeeder {
	private BufferedReader reader;
	
	public IpListFileSeeder(String path, IpCrawler crawler) throws IOException {
		super(crawler);
		reader = new BufferedReader(new FileReader(path));
	}
		
	protected void seed() {
		String line;
		try {
			while ((line = reader.readLine()) != null) {
				try {
					InetAddress addr = InetAddress.getByName(line);
					if (!(addr.isAnyLocalAddress()
							|| addr.isLinkLocalAddress()
							|| addr.isLoopbackAddress()
							|| addr.isMulticastAddress()
							|| addr.isSiteLocalAddress())) {
						add(addr);						
					}
				} catch (UnknownHostException e) {
					System.err.println("Invalid IP address: " + line);
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			System.out.println(toString() + " was interrupted.");
		}
	}
}
