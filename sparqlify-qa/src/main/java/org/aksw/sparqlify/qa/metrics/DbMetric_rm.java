package org.aksw.sparqlify.qa.metrics;

import java.sql.Connection;

public class DbMetric_rm extends MetricImpl implements Metric {
	
	protected Connection conn;

	public void registerDbConnection(Connection conn) {
		this.conn = conn;
	}

}