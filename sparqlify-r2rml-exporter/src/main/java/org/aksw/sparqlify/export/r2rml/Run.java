package org.aksw.sparqlify.export.r2rml;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import javax.sql.DataSource;

import org.aksw.sparqlify.core.RdfViewSystemOld;
import org.aksw.sparqlify.core.datatypes.DatatypeSystem;
import org.aksw.sparqlify.core.domain.input.ViewDefinition;
import org.aksw.sparqlify.util.MapReader;
import org.aksw.sparqlify.util.SparqlifyUtils;
import org.aksw.sparqlify.util.ViewDefinitionFactory;

import com.hp.hpl.jena.rdf.model.Model;

public class Run {
	
	public static void main(String[] args) throws IOException, SQLException {
		// init things
		RdfViewSystemOld.initSparqlifyFunctions();
		DatatypeSystem datatypeSystem = 
				SparqlifyUtils.createDefaultDatatypeSystem();
		
		// get dummy data source and dummy connection
		DataSource dataSource = SparqlifyUtils.createTestDatabase();
		Connection conn = dataSource.getConnection();
		
		/* get type mappings; looks like this:
		 * #h2-type-name   aligned-type-name
		 * bool    boolean
		 * int2    integer
		 * int4    integer
		 * ...
		 * 
		 * and so on
		 */
		Map<String, String> typeAlias = MapReader.readFile(
				new File("src/main/resources/type-map.h2.tsv"));
		
		// creates views...
		ViewDefinitionFactory vdf =
				SparqlifyUtils.createViewDefinitionFactory(conn, typeAlias);
		
		// example view
		ViewDefinition personView =
				vdf.create(
						"Prefix ex:<http://ex.org/> " +
						"Create " +
							"View person " +
						"As " +
							"Construct { ?s a ex:Person ; ex:name ?t } " +
						"With " +
							//"?s = uri(concat('http://ex.org/person/', ?ID) " +
							"?s = plainLiteral(?ID) " +
							"?t = plainLiteral(?NAME) From person"
				);
		
		// another example view
		ViewDefinition deptView =
				vdf.create(
						"Prefix ex:<http://ex.org/> " +
						"Create " +
							"View dept " +
						"As " +
							"Construct { ?s a ex:Department ; ex:name ?t } " +
						"With " +
							"?s = uri(concat('http://ex.org/dept/', ?ID) " +
							"?t = plainLiteral(?NAME)" +
						"From [[SELECT ID, NAME FROM dept " +
						"WHERE ID > 23]]"
				);
		
		// and yet another one
		ViewDefinition personToDeptView =
				vdf.create(
						"Prefix ex:<http://ex.org/> " +
						"Create " +
							"View person_to_dept " +
						"As " +
							"Construct { ?p ex:worksIn ?d } " +
						"With " +
							"?p = uri(concat('http://ex.org/person/', ?PERSON_ID) " +
							"?d = uri(concat('http://ex.org/dept/', ?DEPT_ID) " +
						"From [[SELECT * FROM person_to_dept " +
						"JOIN dept ON person_to_dept.dept_id=dept.id]]"
				);
		
		Collection<ViewDefinition> viewDefs = 
				Arrays.asList(personView, deptView, personToDeptView);
		
		R2RMLExporter exporter = new R2RMLExporter(viewDefs);
		Model r2rml2 = exporter.export();	
		
//		System.out.println();
//		System.out.println("#################################");
//		System.out.println(viewDefs);
	}

}
