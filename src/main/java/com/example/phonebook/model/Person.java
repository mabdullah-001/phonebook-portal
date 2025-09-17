package com.example.phonebook.model;


import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Pattern;


public class Person implements Serializable {

    private static final long serialVersionUID = 1L;


    private Integer id;

    private String name;


    private String phone;

    private String email;

    private String country;

    private String city;


    private String street;



    public static final Comparator<Person> NAME_COMPARATOR =
            Comparator.comparing(Person::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));


    public Person() {
    }


    public Person(Integer id, String name, String phone, String email,
                  String country, String city, String street) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.country = country;
        this.city = city;
        this.street = street;
    }



    /* ------------------------ Getters & Setters ------------------------ */

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Person other)) return false;

        if (this.id != null && other.id != null) {
            return this.id.equals(other.id);
        }
        return Objects.equals(this.phone, other.phone);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return id.hashCode();
        }
        return phone != null ? phone.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Person{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", country='" + country + '\'' +
                ", city='" + city + '\'' +
                ", street='" + street + '\'' +
                '}';
    }
}


