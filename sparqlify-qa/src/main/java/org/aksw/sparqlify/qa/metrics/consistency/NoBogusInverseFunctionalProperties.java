package org.aksw.sparqlify.qa.metrics.consistency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.aksw.sparqlify.core.algorithms.ViewQuad;
import org.aksw.sparqlify.core.domain.input.ViewDefinition;
import org.aksw.sparqlify.qa.exceptions.NotImplementedException;
import org.aksw.sparqlify.qa.metrics.PinpointMetric;
import org.aksw.sparqlify.qa.metrics.TripleMetric;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

/**
 * This metric reports triples having known "bogus owl:inverseFunctionalProperty
 * values" as described in
 *   A. Hogan, A. Harth, A. Passant, S. Decker, and A. Polleres. Weaving the
 *   Pedantic Web. In Linked Data on the Web Workshop (LDOW2010) at WWW'2010,
 *   2010.
 * 
 * Such values stem from not validated inputs, e.g. in the case of the inverse-
 * functional property http://xmlns.com/foaf/0.1/mbox_sha1sum the value
 * "08445a31a78661b5c746feff39a9db6e4e2cc5cf" is the SHA1 encoded string of
 * mailto: . Since http://xmlns.com/foaf/0.1/mbox_sha1sum is inverse-functional
 * one could infer that all the resources having this value assigned refer to
 * the same entity which is wrong.
 * 
 * To find triples having such "bogus owl:inverseFunctionalProperty values" a
 * blacklist is provided and checked for every input triple.
 * 
 * @author Patrick Westphal <patrick.westphal@informatik.uni-leipzig.de>
 *
 */
public class NoBogusInverseFunctionalProperties extends PinpointMetric
		implements TripleMetric {

	private final List<Triple> blackList = new ArrayList<Triple>(Arrays.asList(
			new Triple(
					Node.createVariable("subject"),
					Node.createURI("http://xmlns.com/foaf/0.1/mbox_sha1sum"),
					Node.createLiteral("08445a31a78661b5c746feff39a9db6e4e2cc5cf")),
			new Triple(
					Node.createVariable("subject"),
					Node.createURI("http://xmlns.com/foaf/0.1/mbox_sha1sum"),
					Node.createLiteral("da39a33ee5e6b4b0d3255bfef95601890afd80709")),
			new Triple(
					Node.createVariable("subject"),
					Node.createURI("http://xmlns.com/foaf/0.1/homepage"),
					Node.createURI("http://")),
			new Triple(
					Node.createVariable("subject"),
					Node.createURI("http://xmlns.com/foaf/0.1/mbox_sha1sum"),
					Node.createLiteral("")),
			new Triple(
					Node.createVariable("subject"),
					Node.createURI("http://xmlns.com/foaf/0.1/isPrimaryTopicOf"),
					Node.createURI("http://"))
		));
		

	@Override
	public void assessTriple(Triple triple) throws NotImplementedException {
		
		for (Triple blackListPattern : blackList) {
			if (!blackListPattern.getSubject().isVariable()
					&& !blackListPattern.subjectMatches(triple.getSubject()))
				continue;

			if (!blackListPattern.getPredicate().isVariable()
					&& !blackListPattern.predicateMatches(triple.getPredicate()))
				continue;

			if (!blackListPattern.getObject().isVariable()
					&& !blackListPattern.objectMatches(triple.getObject()))
				continue;
			
			Set<ViewQuad<ViewDefinition>> viewQuads = pinpointer
					.getViewCandidates(triple);
			writeTripleMeasureToSink(0, triple, viewQuads);
		}
	}

}
