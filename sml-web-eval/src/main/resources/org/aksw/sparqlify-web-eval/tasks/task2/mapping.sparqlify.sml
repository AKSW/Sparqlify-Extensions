Prefix xsd:<http://www.w3.org/2001/XMLSchema#>
Prefix ex:<http://example.org/>

Prefix country:<http://downlode.org/rdf/iso-3166/countries#>


Create View people As
  Construct {
    ?s
      a ex:Person ;
      ex:firstName ?fn .
  }
  With
    ?s  = uri(concat(ex:person, ?id))
    ?fn = plainLiteral(?first_name)
  From
    person

