package iptopo;

import iptopo.graph.IpNode;

import java.io.IOException;
import java.net.UnknownHostException;

import java.util.List;

import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.client.remote.OServerAdmin;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;

public class DbClient {
	public static final String db_server = "remote:localhost";
	public static final String db_name = "iptopo";
	public static final String db_user = "root";
	public static final String db_pwd = "ooz6Ciesuxee";
	private OServerAdmin server_admin;
	private OObjectDatabaseTx db;


	public DbClient() throws IOException {
		try {
			server_admin = new OServerAdmin(db_server);
			server_admin.connect(db_user, db_pwd);
			if (!server_admin.listDatabases().containsKey(db_name)) {
				server_admin.createDatabase(db_name, "document", "plocal");
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
		finally {
			server_admin.close();
		}

		db = new OObjectDatabaseTx(db_server + "/" + db_name);
		db.open(db_user, db_pwd);
		db.getEntityManager().registerEntityClasses("iptopo.graph");
	}
	
	public OObjectDatabaseTx getDb() {
		return db;
	}

	public void setDb(OObjectDatabaseTx db) {
		this.db = db;
	}

	public void close() {
		db.close();		
	}
	
	public <RET> List<RET> query(String sql_str) {
		List<RET> rv = db.query(new OSQLSynchQuery<RET>(sql_str));
		return rv;
	}
	
	public void save_node(IpNode node) {
		save_node(node, true);
	}
	
	public void save_node(IpNode node, boolean overwrite_location) {
		final int max_retries = 3;
		int i = 0;
		db.begin();
		while(true) {
			try {
				db.attachAndSave(node);
				db.commit();
				break;
			}
			catch (ONeedRetryException e) {
				if (i >= max_retries) {
					db.rollback();
					throw e;
				}
				else {
					IpNode tmp_node;
					try {
						tmp_node = new IpNode(node.getAddr());
						tmp_node.merge(node, overwrite_location);
						node = db.reload(node);
						node = db.detach(node);
						node.merge(tmp_node, overwrite_location);
					} catch (UnknownHostException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
			i++;
		}
	}
	
	
	public IpNode get_node(String ip_str) throws UnknownHostException {
		IpNode rv;
		db.begin();
		List<IpNode> db_nodes = db.query(new OSQLSynchQuery<IpNode>(
					"select from IpNode where addr = \"" + ip_str + "\""));
		if (db_nodes.size() == 0) {
			rv = db.newInstance(IpNode.class);
			rv.setAddr(ip_str);
			save_node(rv, true);
		}
		else if (db_nodes.size() == 1) {
			rv = db_nodes.get(0);
			rv = db.detach(rv);
		}
		else {
			db.rollback();
			throw new RuntimeException("Database corruption: duplicate IpNode " + ip_str);
		}
		db.commit();
		return rv;
	}

}
