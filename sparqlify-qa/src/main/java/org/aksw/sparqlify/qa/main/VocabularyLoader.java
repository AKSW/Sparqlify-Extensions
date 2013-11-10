package org.aksw.sparqlify.qa.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;

import org.aksw.commons.collections.Pair;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class VocabularyLoader {
	
	private final String pathPrefix = "src/main/resources/vocabularies/";
	private final HashMap<String, Pair<String, String>> knownVocabularies =
			new HashMap<String, Pair<String, String>>(){
				private static final long serialVersionUID = -4601039038754504181L;
			{
				put("http://www.w3.org/1999/02/22-rdf-syntax-ns#",
						new Pair<String, String>("22-rdf-syntax-ns.rdf", "RDF/XML"));
				put("http://www.w3.org/2000/01/rdf-schema#",
						new Pair<String, String>("rdf-schema.rdf", "RDF/XML"));
				put("http://www.w3.org/2002/07/owl#",
						new Pair<String, String>("owl.ttl", "TTL"));
				put("http://xmlns.com/foaf/0.1/",
						new Pair<String, String>("foaf.rdf", "RDF/XML"));
			}};
	
	private final HashMap<String, String> knownPrefixes =
			new HashMap<String, String>(){
				private static final long serialVersionUID = 7477631573380330379L;
			{
				put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
				put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
				put("owl", "http://www.w3.org/2002/07/owl#");
				put("foaf", "http://xmlns.com/foaf/0.1/");
			}};
	
	// model cache for already loaded vocabularies; key is the whole namespace
	private HashMap<String, Model> cache;


	public VocabularyLoader() {
		cache = new HashMap<String, Model>();
	}


	public Model getVocabulary(String prefixOrNamespace) throws FileNotFoundException {
		String namespace;
		if (knownPrefixes.containsKey(prefixOrNamespace))
			namespace = knownPrefixes.get(prefixOrNamespace);
		else
			namespace = prefixOrNamespace;
		
		// check if already cached
		if (cache.containsKey(namespace)) return cache.get(namespace);
		else return loadVocabulary(namespace);
	}


	private Model loadVocabulary(String namespace) throws FileNotFoundException {
		String filePath = pathPrefix + knownVocabularies.get(namespace).first;
		String type = knownVocabularies.get(namespace).second;
		File file = new File(filePath);
		Reader reader = new FileReader(file);
		Model vocab = ModelFactory.createDefaultModel();
		vocab.read(reader, type);
		
		if (!cache.containsKey(namespace)) cache.put(namespace, vocab);
		
		return vocab;
	}


	public Model getVocabularies(Collection<String> prefixesOrNamespaces)
			throws FileNotFoundException {
		
		Model vocabs = ModelFactory.createDefaultModel();
		for (String prefixOrNamespace : prefixesOrNamespaces)
			vocabs.add(getVocabulary(prefixOrNamespace));
		
		return vocabs;
	}


	public Model getAllKnownVocabulariesOf(Collection<String> prefixesOrNamespaces) {
		
		Model vocabs = ModelFactory.createDefaultModel();
		for (String prefixOrNamespace : prefixesOrNamespaces) {
			Model vocab;
			try {
				vocab = getVocabulary(prefixOrNamespace);
			} catch (FileNotFoundException e) {
				continue;
			}
			vocabs.add(vocab);
		}
		
		return vocabs;
	}
}