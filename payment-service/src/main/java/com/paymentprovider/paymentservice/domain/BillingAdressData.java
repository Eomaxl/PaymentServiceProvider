package com.paymentprovider.paymentservice.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public class BillingAdressData {
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String countryCode;

    public BillingAdressData() {}

    public BillingAdressData(String street, String city, String state, String postalCode, String countryCode) {
        this.street = street;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.countryCode = countryCode;
    }

    public static BillingAdressData from(BillingAddress billingAddress){
        if (billingAddress == null){
            return null;
        }
        return new BillingAdressData(
                billingAddress.getStreet(),
                billingAddress.getCity(),
                billingAddress.getState(),
                billingAddress.getPostalCode(),
                billingAddress.getCountryCode()
        );
    }


    public BillingAddress toBillingAddress(){
        return new BillingAddress(street, city, state, postalCode, countryCode);
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
}
