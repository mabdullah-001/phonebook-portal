package com.example.phonebook;

import com.example.phonebook.model.Contact;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Repository for storing contacts in memory.
 * Later we will replace this with a MySQL-backed repository.
 */
public class ContactRepository {

    // In-memory list to hold contacts
    private final List<Contact> contacts = new ArrayList<>();

    /**
     * Return all contacts, sorted by name.
     */
    public List<Contact> findAll() {
        List<Contact> sorted = new ArrayList<>(contacts); // copy to avoid external modification
        sorted.sort(Contact.NAME_COMPARATOR);
        return Collections.unmodifiableList(sorted); // return read-only copy
    }

    /**
     * Find a contact by phone number.
     * @param phone phone number to search
     * @return Optional<Contact> if found, empty otherwise
     */
    public Optional<Contact> findByPhone(String phone) {
        return contacts.stream()
                .filter(c -> phone.equals(c.getPhone()))
                .findFirst();
    }

    /**
     * Add a new contact.
     * Enforces uniqueness of phone number.
     * @param contact the contact to add
     * @throws IllegalArgumentException if phone number already exists
     */
    public void add(Contact contact) {
        if (findByPhone(contact.getPhone()).isPresent()) {
            throw new IllegalArgumentException("Phone number already exists: " + contact.getPhone());
        }
        contacts.add(contact);
    }

    /**
     * Update an existing contact.
     * Uniqueness check ensures no other contact has the same phone.
     * @param contact the updated contact
     */
    public void update(Contact contact) {
        // Find index of existing contact
        for (int i = 0; i < contacts.size(); i++) {
            Contact existing = contacts.get(i);

            if (existing.getId() != null && existing.getId().equals(contact.getId())) {
                // If updating, ensure phone is not taken by another contact
                boolean duplicatePhone = contacts.stream()
                        .anyMatch(c -> !c.equals(existing) && contact.getPhone().equals(c.getPhone()));
                if (duplicatePhone) {
                    throw new IllegalArgumentException("Phone number already exists: " + contact.getPhone());
                }
                contacts.set(i, contact);
                return;
            }
        }
        throw new IllegalArgumentException("Contact not found with id=" + contact.getId());
    }

    /**
     * Delete a contact.
     */
    public void delete(Contact contact) {
        contacts.remove(contact);
    }

    /**
     * Clear all contacts (for testing/demo).
     */
    public void clear() {
        contacts.clear();
    }
}

