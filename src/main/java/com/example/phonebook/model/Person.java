package com.example.phonebook.model;


import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Pattern;


public class Person implements Serializable {

    private static final long serialVersionUID = 1L;


    private Integer id;

    /** Full name of person (we store as single 'name' per requirement). */
    private String name;


    private String phone;

    /** Email address (optional but validated). */
    private String email;

    /** Country name (optional). */
    private String country;

    /** City name (optional). */
    private String city;

    /** Street address (optional). */
    private String street;

    /**
     * Simple email validation pattern.
     * NOTE: this is intentionally simple (covers usual cases). For full RFC compliance, use a specialized library.
     */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");


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


    public Person(String name, String phone, String email,
                   String country, String city, String street) {
        this(null, name, phone, email, country, city, street);
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

    public boolean hasValidEmail() {
        String e = this.email;
        return e != null && !e.isBlank() && EMAIL_PATTERN.matcher(e).matches();
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


