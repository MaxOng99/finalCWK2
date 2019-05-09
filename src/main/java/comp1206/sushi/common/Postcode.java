package comp1206.sushi.common;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

public class Postcode extends Model implements Serializable{
	
	private static final long serialVersionUID = -2179416792423154920L;
	private String name;
	private Map<String,Double> latLong;
	private double distance;

	public Postcode(String code) {
		this.name = code;
		calculateLatLong();
	}
	
	@Override
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public Number getDistance() {
		return this.distance;
	}

	public Map<String,Double> getLatLong() {
		return this.latLong;
	}
	
	public void setDistanceZero() {
		distance = 0;
	}
	/**
	 * @author David George, Neeme Praks (https://stackoverflow.com/questions/3694380)
	 * Calculates the distance between two points on the surface of Earth based on Haversine formula 
	 * @param restaurant This parameter is needed to get the Postcode of the Restaurant
	 */
	public void calculateDistance(Restaurant restaurant) {
		Postcode destination = restaurant.getLocation();
		double restaurantLat = destination.getLatLong().get("lat");
		double restaurantLon = destination.getLatLong().get("lon");
		
		double thisLat = this.getLatLong().get("lat");
		double thisLon = this.getLatLong().get("lon");
		
		final int R = 6371; // Radius of the earth

	    double latDistance = Math.toRadians(restaurantLat - thisLat);
	    double lonDistance = Math.toRadians(restaurantLon - thisLon);
	    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
	            + Math.cos(Math.toRadians(thisLat)) * Math.cos(Math.toRadians(restaurantLat))
	            * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
	    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	    double distance = R * c * 1000; // convert to meters
	    
	    this.distance = distance;    
	}
	
	/**
	 * Calculates the lat/long of a Postcode 
	 */
	public void calculateLatLong() {
		
		//Removes white space in the PostcodeS, because GET parameter requires PostcodeS w/o spaces
		String postcode = this.name.replaceAll("\\s+", "");
		
		JSONObject jsonObj;
		PostcodeAPI api = new PostcodeAPI(postcode);
		try {
			api.sendGetRequest();
			jsonObj = api.getJSONResponse();
			this.latLong = new HashMap<String,Double>();
			
			Double lat = Double.parseDouble(jsonObj.getString("lat"));
			Double lon = Double.parseDouble(jsonObj.getString("long"));
			
			latLong.put("lat", lat);
			latLong.put("lon", lon);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean equals(Object o) {
	    if (o == this) {
	      return true;
	    }
	    if (!(o instanceof Postcode)) {
	      return false;
	    }
	    Postcode postcodeInstance = (Postcode)o;
	    return postcodeInstance.name.equals(name);
	}
	 
	  public int hashCode() {
	    int result = 17;
	    result = 31 * result + name.hashCode();
	    return result;
	}
}
