package org.aksw.sparqlify.qa.main;

import java.io.StringReader;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.aksw.sparqlify.core.domain.input.ViewDefinition;
import org.aksw.sparqlify.qa.dataset.SparqlifyDump;
import org.aksw.sparqlify.qa.dimensions.Dimension;
import org.aksw.sparqlify.qa.exceptions.TripleParseException;
import org.aksw.sparqlify.qa.metrics.DatasetMetric;
import org.aksw.sparqlify.qa.metrics.MappingMetric;
import org.aksw.sparqlify.qa.metrics.MeasureDataSink;
import org.aksw.sparqlify.qa.metrics.Metric;
import org.aksw.sparqlify.qa.metrics.NodeMetric;
import org.aksw.sparqlify.qa.metrics.Pinpointer;
import org.aksw.sparqlify.qa.metrics.TripleMetric;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;



/**
 * The QualityAssessment class is meant to do the actual quality assessment. All
 * it needs to do that are
 * - a Sparqlify dump (SparqlifyDump)
 * - the view definitions (Collection<ViewDefinition>)
 * - access the to database (Connection)
 * - data quality dimensions holding certain metrics to be applied to the data
 *   of the Sparqlify dump (Collection<Dimension> and Collection<Metric>
 *   respectively)
 * - a target to write relevant measure data to (MeasureDataSink)
 * 
 * The following steps are done in the given order:
 * 1) group all metrics according to the interfaces they implement; these are
 *    used detect if the metrics should be applied to
 *    - the whole dataset
 *    - one triple
 *    - one RDF node (resource or literal)
 *    - mappings defined in a Sparqlify view
 * 2) initialize the structures needed to write measured data to the measure
 *    data sink
 * 3) TODO: run all metrics to be applied to the whole dataset and write back
 *    relevant results
 * 4) TODO: iterate triple-wise over the dataset
 *    4.1) TODO: apply all triple metrics to the current triple and write back
 *         relevant results
 *    4.2) TODO: iterate over all triple parts (i.e. subject, predicate, object)
 *         4.2.1) TODO: apply all node metrics (if applicable) to the
 *                current triple part and write back relevant results
 * 
 * TODO: write a method to display the results on the console
 * TODO: write tests
 * 
 * @author Patrick Westphal <patrick.westphal@informatik.uni-leipzig.de>
 *
 */
public class QualityAssessment {
	
	private SparqlifyDump dump;
	private List<Metric> datasetMetrics;
	private List<Metric> tripleMetrics;
	private List<Metric> nodeMetrics;
	private List<Metric> mappingMetrics;


	public QualityAssessment(SparqlifyDump dump,
			Collection<ViewDefinition> viewDefs, Connection conn,
			Collection<Dimension> dims, MeasureDataSink measureDataSink) {
		this.dump = dump;
		
		datasetMetrics = new ArrayList<Metric>();
		tripleMetrics = new ArrayList<Metric>();
		nodeMetrics = new ArrayList<Metric>();
	
	
		for (Dimension dim : dims) {
			Collection<Metric> metrics = dim.getMetrics();
			for (Metric metric : metrics) {
				/*
				 *  1) group all metrics according to the interfaces they
				 *     implement
				 */
				Class<?>[] interfaces = metric.getClass().getInterfaces();
				for (int i=0; i < interfaces.length; i++) {
					Class<?> intfce = interfaces[i];
					String name = intfce.getSimpleName();
					
					if (name.equals(DatasetMetric.class.getSimpleName())) {
						datasetMetrics.add(metric);

					} else if (name.equals(TripleMetric.class.getSimpleName())) {
						tripleMetrics.add(metric);
						

					} else if (name.equals(NodeMetric.class.getSimpleName())) {
						nodeMetrics.add(metric);
					
					} else if (name.equals(MappingMetric.class.getSimpleName())) {
						mappingMetrics.add(metric);
						
					} else continue;
					
					/*
					 * FIXME: update comment
					 *  2) initialize the structures needed to write
					 *     measured data to the measure data sink
					 */
					metric.registerMeasureDataSink(measureDataSink);
					metric.registerDbConnection(conn);
					
					Pinpointer pinpointer = new Pinpointer(viewDefs);
					
					metric.registerPinpointer(pinpointer);
				}
			}
		}
	}
	
	
	public void run() throws TripleParseException {
		boolean runDatasetAssessment = datasetMetrics.size() > 0;
		boolean runTripleAssessment = tripleMetrics.size() > 0;
		boolean runNodeAssessment = nodeMetrics.size() > 0;
		
		if (runDatasetAssessment) {
			assessDataset();
		}
		if (runTripleAssessment || runNodeAssessment) {
			for (String line : dump) {
				Model lineStatement = ModelFactory.createDefaultModel();
				StringReader reader = new StringReader(line);
				lineStatement.read(reader, null,  "TTL");
				// NtripleIterator
				StmtIterator iter = lineStatement.listStatements();
				
				while (iter.hasNext()) {
					Statement triple = iter.next();
					// no optimizations here
					if (runTripleAssessment) assessTriple(triple);
					if (runNodeAssessment) assessNodes(triple);
				}
			}
		}
	}
	
	
	private void assessTriple(Statement statement) {
		for (Metric metric : tripleMetrics) {
			((TripleMetric) metric).assessTriple(statement.asTriple());
		}
	}
	
	
	private void assessNodes(Statement statement) {
		for (Metric metric : nodeMetrics) {
			((NodeMetric) metric).assessNodes(statement.asTriple());
		}
	}
	
	
	private void assessDataset() {
		// TODO: implement
	}
	
	
	private void assessMappings() {
		// TODO: implement
	}
}
