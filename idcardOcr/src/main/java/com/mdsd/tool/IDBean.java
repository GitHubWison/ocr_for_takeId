package com.mdsd.tool;

import androidx.annotation.NonNull;

public class IDBean {
    private String name;
    private String gender;
    private String year;
    private String month;
    private String day;
    private String idNo;

    public IDBean(String name, String gender, String year, String month, String day, String idNo) {
        this.name = name;
        this.gender = gender;
        this.year = year;
        this.month = month;
        this.day = day;
        this.idNo = idNo;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("name=%s;gender=%s;year=%s;month=%s;day=%s;idNo=%s",name,gender,year,month,day,idNo);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getIdNo() {
        return idNo;
    }

    public void setIdNo(String idNo) {
        this.idNo = idNo;
    }

}
