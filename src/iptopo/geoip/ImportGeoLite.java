package iptopo.geoip;

import iptopo.Config;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.net.Inet4Address;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.intent.OIntentMassiveInsert;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

public class ImportGeoLite {
	public static final String db_server = "plocal:" + Config.getDbLocalPath();
	private ODatabaseDocumentTx db;
	private Path csv_path;
	private final static String loc_fname = "GeoLiteCity-Location.csv";
	private final static String net_fname = "GeoLiteCity-Blocks.csv";

	public static void main(String[] args) {
		ImportGeoLite importer = new ImportGeoLite(args[0]);
		try {
			importer.import_CSV();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void ensureProperty(OClass clazz, String name, OType type) {
		if (clazz.getProperty(name) == null) clazz.createProperty(name, type);
	}
	
	public ImportGeoLite(String path) {
		csv_path = Paths.get(path);
		db = new ODatabaseDocumentTx(db_server + "/" + Config.getDbName());
		if (!db.exists()) db.create();
		db.open("admin", "admin");

		try {
			OSchema schema = db.getMetadata().getSchema();

			OClass loc_class = schema.getOrCreateClass("Location");
			ensureProperty(loc_class, "longitude", OType.DOUBLE);
			ensureProperty(loc_class, "latitude", OType.DOUBLE);
			ensureProperty(loc_class, "csvID", OType.INTEGER);
			if (db.getMetadata().getIndexManager().getIndex("location.csvID") == null)
				loc_class.createIndex("location.csvID", OClass.INDEX_TYPE.UNIQUE_HASH_INDEX, "csvID");

			OClass net_class = schema.getOrCreateClass("Network");
			ensureProperty(net_class, "startIP", OType.LONG);
			ensureProperty(net_class, "endIP", OType.LONG);
			ensureProperty(net_class, "location", OType.LINK);
			if (db.getMetadata().getIndexManager().getIndex("Network.startIP") == null)
				net_class.createIndex("Network.startIP", OClass.INDEX_TYPE.UNIQUE, "startIP");

			OClass geoip_meta = schema.getOrCreateClass("GeoIP_meta");
			ensureProperty(geoip_meta, "className", OType.STRING);
			ensureProperty(geoip_meta, "lastUpdated", OType.DATETIME);
			
			OClass node_class = schema.getOrCreateClass("IpNode");
			ensureProperty(node_class, "location", OType.LINK);
		}
		finally {
			db.close();
		}
	}

	public void import_CSV() throws FileNotFoundException, IOException {
		db.open("admin", "admin");
		try {

			// Seriously, one file is encoded in ISO-8859-1, while the other...
			BufferedReader locations_file = Files.newBufferedReader(
					csv_path.resolve(loc_fname), StandardCharsets.ISO_8859_1);
			// ... is encoded in UTF-8. o_O WTF?
			BufferedReader nets_file = Files.newBufferedReader(
					csv_path.resolve(net_fname), StandardCharsets.UTF_8);

			// Skip copyright notices
			locations_file.readLine();
			nets_file.readLine(); 

			long nrec_last = 0, nrec_now;
			float t1, t2, last_out = 0, db_time = 0, csv_time = 0;
			OSQLSynchQuery<ODocument> loc_query;

			///*
			db_write_locs(locations_file);

			
			System.out.println("Importing networks...");			

			db.getMetadata().getSchema().getClass("Network").truncate();
			loc_query = new OSQLSynchQuery<ODocument>(
					"select from Location where csvID = ?");
			CSVParser parser = CSVFormat.DEFAULT.withHeader().parse(nets_file);
			
			t2 = ((float)System.nanoTime()/1000000000);

			for (CSVRecord net_record : parser) {
				Integer loc_id = Integer.valueOf(net_record.get("locId"));
				Long start_ip = Long.valueOf(net_record.get("startIpNum"));
				Long end_ip = Long.valueOf(net_record.get("endIpNum"));
				t1 = ((float)System.nanoTime()/1000000000);
				csv_time += t1 - t2;

				ODocument net_doc = db.newInstance("Network");
				net_doc.field("startIP", start_ip);
				net_doc.field("endIP", end_ip);

				List<ODocument> locs = db.command(loc_query).execute(loc_id);
				if (locs.size() == 0) {
					throw new RuntimeException(
							"missing locId in " + csv_path.toString() + loc_fname + ": " + loc_id);
				}
				ODocument loc_doc = locs.get(0);
				net_doc.field("location", loc_doc);
				net_doc.save();
				t2 = ((float)System.nanoTime()/1000000000);
				db_time += t2 - t1;
				
				if (t2 - last_out >= 2) {
					nrec_now = parser.getRecordNumber();
					System.out.println("Processed records: " + nrec_now
							+ ", csv_time = " + csv_time
							+ ", db_time = " + db_time
							+ ", rec/s = " + ((float)(nrec_now - nrec_last))/(t2 - last_out));
					nrec_last = nrec_now;
					last_out = t2;
				}
			}

			//*/
			System.out.println("Linking nodes to networks...");
			
			loc_query = new OSQLSynchQuery<ODocument>(
					"select location from Network where startIP <= ? order by startIP desc limit 1");
			
			last_out = 0;
			db_time = 0;
			csv_time = 0;
			t1 = ((float)System.nanoTime()/1000000000);
			
			long count = db.countClass("IpNode");
			nrec_now = 0;
			for (ODocument node_doc : db.browseClass("IpNode")) {
				byte[] addr_bin = Inet4Address.getByName((String)node_doc.field("addr")).getAddress();
				Long addr = 0l;
				for (int i=0; i < 4; i++) {
					Long v;
					if (addr_bin[i] < 0) {
						v = 256l + addr_bin[i];
					}
					else {
						v = (long)addr_bin[i];
					}
					addr |= Long.valueOf(v << (8*(3-i)));
				}
				List<ODocument> locs = db.command(loc_query).execute(addr);
				if (locs.size() == 1) {
					node_doc.field("location", locs.get(0).field("location"));
				}
				node_doc.save();
				t2 = ((float)System.nanoTime()/1000000000);
				db_time += t2 - t1;

				++nrec_now;
				if (t2 - last_out >= 2) {
					System.out.println("Processed records: " + nrec_now + "/" + count
							+ ", db_time = " + db_time
							+ ", rec/s = " + ((float)(nrec_now - nrec_last))/(t2 - last_out));
					nrec_last = nrec_now;
					last_out = t2;
				}
				t1 = t2;
			}
			db.declareIntent(null);

		}
		catch (RuntimeException e) {
			db.rollback();
			throw e;    		
		}
		finally {
			db.close();
		}
	}

	private void db_write_locs(Reader input) throws IOException {
		System.out.println("Importing locations...");
		
		db.getMetadata().getSchema().getClass("Location").truncate();
		db.declareIntent( new OIntentMassiveInsert() );
		CSVParser parser = CSVFormat.DEFAULT.withHeader().parse(input);
		
		long nrec_last = 0, nrec_now;
		float t1, t2, last_out = 0, db_time = 0, csv_time = 0;
		t2 = ((float)System.nanoTime()/1000000000);
		
		for (CSVRecord loc_record : parser) {
			Integer id = Integer.parseInt(loc_record.get("locId"));
			Double longitude = Double.valueOf(loc_record.get("longitude"));
			Double latitude = Double.valueOf(loc_record.get("latitude"));
			t1 = ((float)System.nanoTime()/1000000000);
			csv_time += t1 - t2;
			
			ODocument loc_doc = db.newInstance("Location");
			loc_doc.field("longitude", longitude);
			loc_doc.field("latitude", latitude);
			loc_doc.field("csvID", id);
			loc_doc.save();
			t2 = ((float)System.nanoTime()/1000000000);
			db_time += t2 - t1;
			
			if (t2 - last_out >= 2) {
				nrec_now = parser.getRecordNumber();
				System.out.println("Processed records: " + nrec_now
						+ ", csv_time = " + csv_time
						+ ", db_time = " + db_time
						+ ", rec/s = " + ((float)(nrec_now - nrec_last))/(t2 - last_out));
				nrec_last = nrec_now;
				last_out = t2;
			}
		}
	}

}
