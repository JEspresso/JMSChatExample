package com.jmr.txn.bean;

/**
 * TODO Put here a description of what this class does.
 * RealEstatesBean Model has Real Estate Transaction properties such as street, city, zip etc. This class is 
 * implemented as a bean and will hold Real Estate Transaction information
 * @author Kevin.
 *         Created Jan 16, 2018.
 */

public class RealEstatesBean {
	
	//RealEstatesBean instace variables
	private String street;
	private String city;
	private int zip;
	private String state;
	private String beds;
	private String baths;
	private int sqFeet;
	private String type;
	private String saleDate;
	private double price;
	private String latitude;
	private String longitude;
	
	//Parameterized constructor
	public RealEstatesBean(String street, String city, int zip, String state, String beds, String baths, int sqFeet, String type, String saleDate, double price, String latitude, String longitude) {//, String state, String beds, String baths, double sqFeet, String type, Date saleDate, double price, String latitude, String longitude) {
		this.street = street;
		this.city = city;
		this.zip = zip;
		this.state = state;
		this.beds = beds;
		this.baths = baths;
		this.sqFeet = sqFeet;
		this.type = type;
		this.saleDate = saleDate;
		this.price = price;
		this.latitude = latitude;
		this.longitude = longitude;
	}

	//Getters and Setters for the RealEstatesBean class
	/**
	 * Returns the value of the field called 'street'.
	 * @return Returns the street.
	 */
	public String getStreet() {
		return this.street;
	}

	/**
	 * Sets the field called 'street' to the given value.
	 * @param street The street to set.
	 */
	public void setStreet(String street) {
		this.street = street;
	}

	/**
	 * Returns the value of the field called 'city'.
	 * @return Returns the city.
	 */
	public String getCity() {
		return this.city;
	}

	/**
	 * Sets the field called 'city' to the given value.
	 * @param city The city to set.
	 */
	public void setCity(String city) {
		this.city = city;
	}

	/**
	 * Returns the value of the field called 'zip'.
	 * @return Returns the zip.
	 */
	public int getZip() {
		return this.zip;
	}

	/**
	 * Sets the field called 'zip' to the given value.
	 * @param zip The zip to set.
	 */
	public void setZip(int zip) {
		this.zip = zip;
	}

	/**
	 * Returns the value of the field called 'state'.
	 * @return Returns the state.
	 */
	public String getState() {
		return this.state;
	}

	/**
	 * Sets the field called 'state' to the given value.
	 * @param state The state to set.
	 */
	public void setState(String state) {
		this.state = state;
	}

	/**
	 * Returns the value of the field called 'beds'.
	 * @return Returns the beds.
	 */
	public String getBeds() {
		return this.beds;
	}

	/**
	 * Sets the field called 'beds' to the given value.
	 * @param beds The beds to set.
	 */
	public void setBeds(String beds) {
		this.beds = beds;
	}

	/**
	 * Returns the value of the field called 'baths'.
	 * @return Returns the baths.
	 */
	public String getBaths() {
		return this.baths;
	}

	/**
	 * Sets the field called 'baths' to the given value.
	 * @param baths The baths to set.
	 */
	public void setBaths(String baths) {
		this.baths = baths;
	}

	/**
	 * Returns the value of the field called 'sqFeet'.
	 * @return Returns the sqFeet.
	 */
	public int getSqFeet() {
		return this.sqFeet;
	}

	/**
	 * Sets the field called 'sqFeet' to the given value.
	 * @param sqFeet The sqFeet to set.
	 */
	public void setSqFeet(int sqFeet) {
		this.sqFeet = sqFeet;
	}

	/**
	 * Returns the value of the field called 'type'.
	 * @return Returns the type.
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * Sets the field called 'type' to the given value.
	 * @param type The type to set.
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Returns the value of the field called 'saleDate'.
	 * @return Returns the saleDate.
	 */
	public String getSaleDate() {
		return this.saleDate;
	}

	/**
	 * Sets the field called 'saleDate' to the given value.
	 * @param saleDate The saleDate to set.
	 */
	public void setSaleDate(String saleDate) {
		this.saleDate = saleDate;
	}

	/**
	 * Returns the value of the field called 'price'.
	 * @return Returns the price.
	 */
	public double getPrice() {
		return this.price;
	}

	/**
	 * Sets the field called 'price' to the given value.
	 * @param price The price to set.
	 */
	public void setPrice(double price) {
		this.price = price;
	}

	/**
	 * Returns the value of the field called 'latitude'.
	 * @return Returns the latitude.
	 */
	public String getLatitude() {
		return this.latitude;
	}

	/**
	 * Sets the field called 'latitude' to the given value.
	 * @param latitude The latitude to set.
	 */
	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	/**
	 * Returns the value of the field called 'longitude'.
	 * @return Returns the longitude.
	 */
	public String getLongitude() {
		return this.longitude;
	}

	/**
	 * Sets the field called 'longitude' to the given value.
	 * @param longitude The longitude to set.
	 */
	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}
}

