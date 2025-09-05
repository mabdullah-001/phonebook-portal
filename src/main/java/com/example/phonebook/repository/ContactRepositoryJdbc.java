package com.example.phonebook.repository;

import com.example.phonebook.ContactRepository;
import com.example.phonebook.model.Contact;
import com.example.phonebook.db.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ContactRepositoryJdbc extends ContactRepository {

    @Override
    public List<Contact> findAll() {
        List<Contact> contacts = new ArrayList<>();
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

    @Override
    public Optional<Contact> findByPhone(String phone) {
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

    @Override
    public void add(Contact contact) {
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

    @Override
    public void update(Contact contact) {
        String sql = "UPDATE contacts SET name=?, email=?, country=?, city=?, street=? WHERE phone=?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, contact.getName());
            stmt.setString(2, contact.getEmail());
            stmt.setString(3, contact.getCountry());
            stmt.setString(4, contact.getCity());
            stmt.setString(5, contact.getStreet());
            stmt.setString(6, contact.getPhone());
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(Contact contact) {
        String sql = "DELETE FROM contacts WHERE phone=?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, contact.getPhone());
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Contact mapRow(ResultSet rs) throws SQLException {
        return new Contact(
                rs.getString("name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("country"),
                rs.getString("city"),
                rs.getString("street")
        );
    }
}

