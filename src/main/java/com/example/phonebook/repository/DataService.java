package com.example.phonebook.repository;

import com.example.phonebook.lock.Broadcaster;
import com.example.phonebook.model.Person;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DataService {

    // --- Singleton instance so cache is shared across all users
    private static final DataService INSTANCE = new DataService();

    // Public accessor for the singleton instance
    public static DataService getInstance() {
        return INSTANCE;
    }
    private final PersonRepositoryJDBC repository = new PersonRepositoryJDBC();

    // New in-memory indexes for O(1) uniqueness checks
    private final ConcurrentMap<String, Person> phoneIndex = new ConcurrentHashMap<>();  //Used by the binder validator and by save() method.
    private final ConcurrentMap<Integer, String> idToPhone = new ConcurrentHashMap<>(); //To update/delete cache correctly, you must know the old phone number for that ID.

    // Constructor
    public DataService() {
        reloadCache(); // build cache once at startup
    }

    private synchronized void reloadCache() {
        phoneIndex.clear();
        idToPhone.clear();

        List<Person> all = repository.findAll();
        for (Person p : all) {
            if (p.getPhone() != null) {
                phoneIndex.put(p.getPhone(), p);
            }
            if (p.getId() != null && p.getPhone() != null) {
                idToPhone.put(p.getId(), p.getPhone());
            }
        }


    }

    // Paginated fetch for Vaadin
    public List<Person> findAll(int offset, int limit) {
        // JDBC repository doesn’t support pagination, so do it in-memory
        List<Person> all = repository.findAll();
        int toIndex = Math.min(offset + limit, all.size());
        if (offset > toIndex) {
            return List.of();
        }
        return all.subList(offset, toIndex);
    }

    public List<Person> findAll() {
        return repository.findAll();
    }



    // Count all records
    public long count() {
        return repository.findAll().size(); // can be optimized with SELECT COUNT(*)
    }

    // Save contact (insert or update)
    public synchronized void save(Person contact) {

        String phone = contact.getPhone();

        Integer id = contact.getId();

        if (id == null) {
            // INSERT
            repository.add(contact);

            // get generated id back from DB
            Optional<Person> saved = repository.findByPhone(phone);
            saved.ifPresent(p -> {
                contact.setId(p.getId());
                phoneIndex.put(phone, p);
                idToPhone.put(p.getId(), phone);
                Broadcaster.broadcast("DATA_UPDATED");
            });

        } else {
            // UPDATE
            String oldPhone = idToPhone.get(id);
            repository.update(contact);

            if (oldPhone != null && !oldPhone.equals(phone)) {
                phoneIndex.remove(oldPhone);
            }
            phoneIndex.put(phone, contact);
            idToPhone.put(id, phone);
            Broadcaster.broadcast("DATA_UPDATED");
        }
    }

    // Delete contact
    public synchronized void delete(Person contact) {
        if (contact == null) return;

        repository.delete(contact);

        if (contact.getPhone() != null) {
            phoneIndex.remove(contact.getPhone());
        }
        if (contact.getId() != null) {
            idToPhone.remove(contact.getId());
        }

        Broadcaster.broadcast("DATA_UPDATED");
    }

    // Find by phone
    public Optional<Person> findByPhone(String phone) {
        return repository.findByPhone(phone);
    }

    public Optional<Person> findById(Integer id) {
        return repository.findById(id);
    }

    public Person getFromCache(String phone) {
        return phoneIndex.get(phone); // O(1)
    }


}
