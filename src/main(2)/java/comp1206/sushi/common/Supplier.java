package comp1206.sushi.common;

import java.io.Serializable;

import comp1206.sushi.common.Supplier;

public class Supplier extends Model implements Serializable{

	private static final long serialVersionUID = -7863156125560967252L;
	private String name;
	private Postcode postcode;
	private Number distance;

	public Supplier(String name, Postcode postcode) {
		this.name = name;
		this.postcode = postcode;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.notifyUpdate("Name", this.name, name);
		this.name = name;
	}

	public Postcode getPostcode() {
		return this.postcode;
	}
	
	public void setPostcode(Postcode postcode) {
		this.notifyUpdate("Postcode", this.postcode, postcode);
		this.postcode = postcode;
	}

	public Number getDistance() {
		return postcode.getDistance();
	}

}
