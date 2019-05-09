package comp1206.sushi.common;

import java.io.BufferedReader;


import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONObject;

/**
 * @author Yi Cheng Ong
 * Class that is responsible for getting the lat/long of a Postcode
 */
public class PostcodeAPI {
	
	private String selectedPostcode;
	private String apiURL;
	private String jsonResponse;
	
	/**
	 * Initialise the URL and sets the query to get lat/long of a specified Postcode
	 * @param postcode The specified Postcode as query
	 */
	public PostcodeAPI(String postcode) {
		selectedPostcode = postcode;
		apiURL = "https://www.southampton.ac.uk/~ob1a12/postcode/postcode.php?postcode="+selectedPostcode;
	}
	
	/**
	 * @author Pankaj (https://www.journaldev.com/7148)
	 * Sends a GET request to the web API to obtain the lat/long of the specified Postcode,
	 * reads the response (in JSON format), and store it as a String
	 * @throws IOException Throws this exception when connection to web API is not successful
	 */
	public void sendGetRequest() throws IOException {
		URL url = new URL(apiURL);
		HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
		urlConnection.setRequestMethod("GET");
		int responseCode = urlConnection.getResponseCode();
		
		if (responseCode == HttpsURLConnection.HTTP_OK) {
			BufferedReader bf = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			String line;
			StringBuffer response = new StringBuffer();
			
			while((line = bf.readLine()) != null) {
				response.append(line);
			}
			
			bf.close();
			jsonResponse = response.toString();
		}
		else {
			throw new IOException("Error connecting the web API");
		}
	}
	
	/*
	 * Parse the response into a JSONOBject and returns it.
	 */
	public JSONObject getJSONResponse() {
		return new JSONObject(jsonResponse);
	}
}
