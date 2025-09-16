package com.example.phonebook.repository;

import com.example.phonebook.db.Database;
import com.example.phonebook.model.Person;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PersonRepositoryJDBC {


    public List<Person> findAll() {
        List<Person> contacts = new ArrayList<>();
        String sql = "SELECT * FROM contacts ORDER BY name";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                contacts.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return contacts;
    }





    public Optional<Person> findByPhone(String phone) {
        String sql = "SELECT * FROM contacts WHERE phone = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, phone);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<Person> findById(Integer id) {
        String sql = "SELECT * FROM contacts WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }


    public void add(Person contact) {
        String sql = "INSERT INTO contacts (name, phone, email, country, city, street) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, contact.getName());
            stmt.setString(2, contact.getPhone());
            stmt.setString(3, contact.getEmail());
            stmt.setString(4, contact.getCountry());
            stmt.setString(5, contact.getCity());
            stmt.setString(6, contact.getStreet());
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void update(Person contact) {
        String sql = "UPDATE contacts SET name=?, phone=?,email=?, country=?, city=?, street=? WHERE id=?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, contact.getName());
            stmt.setString(2, contact.getPhone());
            stmt.setString(3, contact.getEmail());
            stmt.setString(4, contact.getCountry());
            stmt.setString(5, contact.getCity());
            stmt.setString(6, contact.getStreet());
            stmt.setInt(7,  contact.getId());
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void delete(Person contact) {
        String sql = "DELETE FROM contacts WHERE id=?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, contact.getId());
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Person mapRow(ResultSet rs) throws SQLException {
        Person person = new Person();
        person.setId(rs.getInt("id"));
        person.setName(rs.getString("name"));
        person.setPhone(rs.getString("phone"));
        person.setEmail(rs.getString("email"));
        person.setCountry(rs.getString("country"));
        person.setCity(rs.getString("city"));
        person.setStreet(rs.getString("street"));
        return person;
    }
}
