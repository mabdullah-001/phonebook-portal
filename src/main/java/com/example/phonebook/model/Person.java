package com.example.phonebook.model;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Represents a contact in the Phonebook.
 *
 * - Mutable plain Java object (POJO) so Vaadin Binder can bind to it.
 * - id is Integer (nullable) so new objects can exist before persistence assigns an id.
 * - phone is expected to be unique (uniqueness enforced in the service layer).
 */
public class Contact implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Database / repository id. Nullable while the contact is not yet persisted.
     */
    private Integer id;

    /** Full name of person (we store as single 'name' per requirement). */
    private String name;

    /** Phone number (must be unique among contacts). */
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

    /**
     * Comparator used to sort contacts by name (case-insensitive).
     * Null names are ordered last.
     */
    public static final Comparator<Contact> NAME_COMPARATOR =
            Comparator.comparing(Contact::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

    /* ------------------------ Constructors ------------------------ */

    /**
     * No-arg constructor is required by many frameworks and is needed for Vaadin Binder.
     */
    public Contact() {
    }

    /**
     * Full constructor including id.
     */
    public Contact(Integer id, String name, String phone, String email,
                   String country, String city, String street) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.country = country;
        this.city = city;
        this.street = street;
    }

    /**
     * Convenience constructor without id (useful before persistence assigns an id).
     */
    public Contact(String name, String phone, String email,
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

    /* ------------------------ Utility / Validation ------------------------ */

    /**
     * Returns true if this contact's email is a non-empty string and matches the EMAIL_PATTERN.
     * Returns false if email is null, empty, or does not match.
     */
    public boolean hasValidEmail() {
        String e = this.email;
        return e != null && !e.isBlank() && EMAIL_PATTERN.matcher(e).matches();
    }

    /* ------------------------ equals / hashCode / toString ------------------------ */

    /**
     * Equality is based primarily on 'id' when available (persisted objects),
     * otherwise falls back to 'phone' which we treat as a logical unique key.
     *
     * Rationale:
     * - When object is persisted (id != null) the id is definitive and stable.
     * - Before persistence the id may be null; phone is supposed to be unique,
     *   so comparing by phone makes it possible to detect duplicates locally.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Contact)) return false;
        Contact other = (Contact) o;

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
        return "Contact{" +
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

