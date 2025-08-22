package com.example.optimaai.data.models;

import java.util.Date;

public class Business {
    private String BusinessName;
    private Date DateEstablished;
    private String Description;

    public Business(String businessName, Date dateEstablished, String description) {
        this.BusinessName = businessName;
        this.DateEstablished = dateEstablished;
        this.Description = description;
    }

    public String getBusinessName() {
        return BusinessName;
    }

    public void setBusinessName(String businessName) {
        BusinessName = businessName;
    }

    public Date getDateEstablished() {
        return DateEstablished;
    }

    public void setDateEstablished(Date dateEstablished) {
        DateEstablished = dateEstablished;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }
}
