package org.linkedgeodata.usertags.core;

public class Tag {
	private String k;
	private String v;
	
	public Tag() {
	}
	
	public Tag(String k, String v) {
		super();
		this.k = k;
		this.v = v;
	}

	public String getK() {
		return k;
	}

	public String getV() {
		return v;
	}
}