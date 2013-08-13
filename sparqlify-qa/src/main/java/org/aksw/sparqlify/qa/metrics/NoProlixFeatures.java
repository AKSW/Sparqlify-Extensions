package org.aksw.sparqlify.qa.metrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.aksw.sparqlify.core.algorithms.ViewQuad;
import org.aksw.sparqlify.core.domain.input.ViewDefinition;
import org.aksw.sparqlify.qa.exceptions.NotImplementedException;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Node_URI;
import com.hp.hpl.jena.graph.Triple;

/**
 * This metric should find statements using RDF features that are more or less
 * deprecated. These are reification, containers and collections:
 * 
 * - RDF reification:
 *   - predicates:
 *     - rdf:subject
 *     - rdf:predicate
 *     - rdf:object
 *   - classes:
 *     - rdf:Statement
 * 
 * - RDF containers:
 *   - predicates:
 *     - rdf:_n (n in natural numbers)
 *     - rdfs:member
 *   - classes:
 *     - rdf:Alt
 *     - rdf:Bag
 *     - rdf:Seq
 *     - rdfs:Container
 * 
 * - RDF collections:
 *   - predicates:
 *     - rdf:first
 *     - rdf:rest
 *   - classes
 *     - rdf:List
 * 
 * TODO: to also consider sub classes of the predicates and all
 * ContainerMembershipProperties (rdf:_2, rdf:_3, ...) this would have to be a
 * dataset measure 
 * 
 * @author Patrick Westphal <patrick.westphal@informatik.uni-leipzig.de>
 *
 */
public class NoProlixFeatures extends PinpointMetric implements TripleMetric {

	private String rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	private String rdfs = "http://www.w3.org/2000/01/rdf-schema#";

	// predicates to look for
	private Node_URI rdf_type = (Node_URI) Node.createURI(rdf+"type");
	private Node_URI rdf_subject = (Node_URI) Node.createURI(rdf+"subject");
	private Node_URI rdf_predicate = (Node_URI) Node.createURI(rdf+"predicate");
	private Node_URI rdf_object = (Node_URI) Node.createURI(rdf+"object");

	private Node_URI rdf__1 = (Node_URI) Node.createURI(rdf+"_1");
	private Node_URI rdfs_member = (Node_URI) Node.createURI(rdfs+"member");

	private Node_URI rdf_first = (Node_URI) Node.createURI(rdf+"first");
	private Node_URI rdf_rest = (Node_URI) Node_URI.createURI(rdf+"rest");

	List<Node_URI> predicates = new ArrayList<Node_URI>(Arrays.asList(
			rdf_subject, rdf_predicate, rdf_object, rdf__1, rdfs_member, rdf_first,
			rdf_rest));

	// classes to look for
	private Node rdf_Statement = Node.createURI(rdf+"Statement");

	private Node rdf_Alt = Node.createURI(rdf+"Alt");
	private Node rdf_Bag = Node.createURI(rdf+"Bag");
	private Node rdf_Seq = Node.createURI(rdf+"Seq");
	private Node rdf_Container = Node.createURI(rdfs+"Container");

	private Node rdf_List = Node.createURI(rdf+"List");
	
	List<Node> classes = new ArrayList<Node>(Arrays.asList(
			rdf_Statement, rdf_Alt, rdf_Bag, rdf_Seq, rdf_Container, rdf_List));


	@Override
	public void assessTriple(Triple triple) throws NotImplementedException {
		findPredicates(triple);
		findClasses(triple);
		
	}


	private void findPredicates(Triple triple) throws NotImplementedException {
		for (Node_URI predicate : predicates) {
			if (triple.getPredicate().equals(predicate)) {
				
				Set<ViewQuad<ViewDefinition>> viewQuads =
						pinpointer.getViewCandidates(triple);
				
				writeTripleMeasureToSink(0, triple, viewQuads);
			}
		}
		
	}


	private void findClasses(Triple triple) throws NotImplementedException {
		for (Node cls : classes) {
			if (triple.getObject().equals(cls) && triple.getPredicate().equals(rdf_type)) {
				
				Set<ViewQuad<ViewDefinition>> viewQuads =
						pinpointer.getViewCandidates(triple);
				
				writeTripleMeasureToSink(0, triple, viewQuads);
			}
		}
	}
}
