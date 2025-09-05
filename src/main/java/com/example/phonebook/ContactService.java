package com.example.phonebook;

import com.example.phonebook.model.Contact;
import com.example.phonebook.repository.ContactRepositoryJdbc;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for Contact operations.
 * Applies business rules (validation, uniqueness) and delegates storage to the repository.
 */
public class ContactService {

    //private final ContactRepository repository; for in memory storage
    private final ContactRepositoryJdbc repository;


    // Constructor injection (in memory storage)
    /*public ContactService(ContactRepository repository) {
        this.repository = repository;
    }*/
    //Constructor injection (for MySQL db )
    public ContactService(ContactRepositoryJdbc repository) {
        this.repository = repository;
    }

    /**
     * Get all contacts, sorted by name.
     */
    public List<Contact> getAllContacts() {
        return repository.findAll();
    }

    /**
     * Find contact by phone number.
     */
    public Optional<Contact> getContactByPhone(String phone) {
        return repository.findByPhone(phone);
    }

    /**
     * Add a new contact with validation.
     */
    public void addContact(Contact contact) {
        validateContact(contact);
        repository.add(contact);
    }

    /**
     * Update existing contact with validation.
     */
    public void updateContact(Contact contact) {
        validateContact(contact);
        repository.update(contact);
    }

    /**
     * Delete a contact.
     */
    public void deleteContact(Contact contact) {
        repository.delete(contact);
    }

    /**
     * Validation rules:
     * - Name must not be null/blank
     * - Phone must not be null/blank
     * - Email must be valid if provided
     */
    private void validateContact(Contact contact) {
        if (contact.getName() == null || contact.getName().isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (contact.getPhone() == null || contact.getPhone().isBlank()) {
            throw new IllegalArgumentException("Phone cannot be empty");
        }
        if (!contact.hasValidEmail() && contact.getEmail() != null && !contact.getEmail().isBlank()) {
            throw new IllegalArgumentException("Invalid email format: " + contact.getEmail());
        }
    }
}

