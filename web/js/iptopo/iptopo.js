/**
 * Map interface to the IP topography stored in OrientDB
 * @param db_url An OrientDB URL that points to the IpTopo database
 * @param map An initialized OpenLayers 3 map object
 */
function IpTopo(map, opt_db_url) {
	var nodes_by_rid = {};
	var processed_nodes_by_rid = {};
	var routes_layer = {};
	var nodes_by_fid = {};
	var loc_features = [];
	
	var q_loc = 'select @rid, addr, longitude, latitude, inPeers, outPeers, $c from IpNode '
		  + 'let $c = format("%f %f", latitude, longitude) group by $c';
	var q_all = 'select @rid, addr, longitude, latitude, inPeers, outPeers from IpNode';

	opt_db_url = opt_db_url || 'http://localhost:2480/iptopo';
	var db = new ODatabase(opt_db_url);

	db.open('reader', 'reader');
	if (db.getErrorMessage() != null) {
		throw new Error(db.getErrorMessage());
	}

	
	/**
	 * @param query An OrientDB query to be run on the database.
	 * @param func(item) The function to be executed on each of the query results
	 */
	// PRIVATE
	function foreach_result(query, func) {
		var q_res = db.query(query, '100000');
		if (q_res) {
			for (var i=0, item; item = q_res["result"][i++];) {
				func(item);
			}
		}
		else {
			throw new Error(db.getErrorMessage());
		}
	};
	
	
	// PRIVATE
	function load_route_features(from_node, direction, rv) {
		if (!processed_nodes_by_rid[from_node.rid]) {
			processed_nodes_by_rid[from_node.rid] = from_node;
			var from_point = ol.proj.transform(
					[from_node.longitude, from_node.latitude],
					ol.proj.get("EPSG:4326"),
					map.getView().getProjection()
			);

			var to_rid;
			var i = 0;
			while ( (to_rid = from_node[direction][i]) ) {
				var to_node = nodes_by_rid[to_rid];
				if (to_node) {
					var to_point = ol.proj.transform(
							[to_node.longitude, to_node.latitude],
							ol.proj.get("EPSG:4326"),
							map.getView().getProjection()
					);
					var f = new ol.Feature({
						geometry: new ol.geom.LineString([from_point, to_point])
					});
					if (direction == 'inPeers') {
						f.setStyle(new ol.style.Style({
							stroke: new ol.style.Stroke({
								color: '#CC9133'
							})							
						}));
					}
					rv.push(f);
					load_route_features(to_node, direction, rv);
				}
				i++;
			}
			delete processed_nodes_by_rid[from_node.rid];
		}
	};
	
	
	// PUBLIC
	this.get_node = function(rid) {
		return nodes_by_rid[rid];
	};
	
	
	/**
	 * @return The OpenLayers 3 map object referenced by this IpTopo instance.
	 */
	// PUBLIC
	this.get_map = function() {
		return map;
	};
	
	
	/**
	 * @return All unique map coordinates that have one or more IP nodes associated with them.
	 */
	// PUBLIC
	this.get_point_features = function() {
		return loc_features;
	};
	
	
	/**
	 * Draw line features along the routes going into and out of a certain node.
	 * Normally called by a private event handler when a node is clicked.
	 * @param from_node originating IP node
	 */
	// PUBLIC
	this.update_routes_layer = function(from_node) {
		var route_features = [];
		load_route_features(from_node, "outPeers", route_features);
		load_route_features(from_node, "inPeers", route_features);
		routes_layer.getSource().addFeatures(route_features);
	};
	
	
	/**
	 * Event handler
	 */
	// PRIVATE
	function on_feature_select(evt) {
		routes_layer.getSource().clear();
		close_menu(document.getElementById('node_menu'));
		this.select_interaction.getFeatures().forEach(
				function(f) {
					if (f.getGeometry() instanceof ol.geom.Point) {
						var nodes = nodes_by_fid[f.getId()];
						if (nodes.length > 1) {
							popup_node_menu(nodes, 
									map.getPixelFromCoordinate(
											f.getGeometry().getCoordinates()), this);
						}
						else {
							for (var i=0, node; node = nodes[i++];) {
								this.update_routes_layer(node);							
							}
						}
					}
				},
				this
		);
	}
	
	
	function close_menu(menu) {
		// while (menu.hasChildNodes()) menu.removeChild(menu.lastChild);
		menu.style.display = 'none';
	}
	
	
	function popup_node_menu(nodes, pixel, self_ref) {
		var menu = document.getElementById('node_menu');
		close_menu(menu);
		var select = document.getElementById('node_select');
		while(select.hasChildNodes()) select.removeChild(select.lastChild);
		select.multiple = true;
		select.size = nodes.length;
		select.style.width = "150px";
		menu.style.width = "150px";
		select.addEventListener('change', function(evt) {
			routes_layer.getSource().clear();
			var sel = evt.target.selectedOptions;
			for (var j=0; j < sel.length; j++) {
				self_ref.update_routes_layer(sel[j].iptopo_node);
			}
		});
		for (var i=0, node; node = nodes[i++];) {
			var p = document.createElement("OPTION");
			p.iptopo_node = node;
			p.innerHTML = node.addr;
			select.appendChild(p);
		}
		menu.appendChild(select);
		with (menu.style) {
			position = 'absolute';
			display = 'inline';
			left = Math.round(pixel[0] + 12) + "px";
			top = Math.round(pixel[1] + 12) + 'px';
		}
	}

	
	/**
	 * Add an IpTopo layer to the given OpenLayers map
	 * @param map An OpenLayers 3 map object
	 */
	// PUBLIC
	this.create_map_layer = function() {
		this.get_map().addLayer(new ol.layer.Vector({
			source: new ol.source.Vector({
				features: this.get_point_features()
			})
		}));
		this.select_interaction = new ol.interaction.Select({
			condition: ol.events.condition.click
		});
		this.get_map().addInteraction(this.select_interaction);
		this.select_interaction.getFeatures().on('change:length', on_feature_select, this);
		this.select_interaction.getFeatures().on('add', on_feature_select, this);
		this.select_interaction.getFeatures().on('remove', on_feature_select, this);
		this.get_map().on('moveend', function(evt) {
			close_menu(document.getElementById('node_menu'));
		}, this);
		routes_layer = new ol.layer.Vector({
			source: new ol.source.Vector()
		});
		this.get_map().addLayer(routes_layer);
	};
	
	
	
	foreach_result(q_all, function(node) {
		// Load the nodes_by_rid table from the database
		nodes_by_rid[node.rid] = node;

		var p = new ol.geom.Point(ol.proj.transform(
				[node.longitude, node.latitude],
				ol.proj.get("EPSG:4326"),
				map.getView().getProjection()
		));
		var fid = "f_" + p.getCoordinates();
		if (!nodes_by_fid[fid]) {
			// No feature representing this node yet
			var f = new ol.Feature({
				geometry: p 
			});
			f.setId(fid);
			nodes_by_fid[fid] = [node];
			loc_features.push(f);	
		}
		else {
			// Already have a feature that represents the same coordinates
			nodes_by_fid[fid].push(node);
		}
	});
			

}







