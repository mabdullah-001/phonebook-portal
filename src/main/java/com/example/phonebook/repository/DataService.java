package com.example.phonebook.repository;

import com.example.phonebook.model.Person;
import com.example.phonebook.repository.exception.StaleDataException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

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

    // --- Global version counter for dirty flag ---
    private final AtomicLong version = new AtomicLong(0);

    // --- Active record locks (per record) ---
    // Key = record id, Value = userId/sessionId
    //private final ConcurrentMap<Integer, String> activeLocks = new ConcurrentHashMap<>();

    private final List<Person> inMemoryDb =
            java.util.Collections.synchronizedList(new java.util.ArrayList<>());
    // Constructor
    public DataService() {
        //reloadCache(); // build cache once at startup
    }

    // Accessors for PersonDataProvider
    public List<Person> getInMemory() {
        return inMemoryDb;
    }


    public AtomicLong getVersionAtomic() {
        return version;
    }

    /*private synchronized void reloadCache() {
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


    }*/

    public synchronized void reloadFromDatabase() {
        phoneIndex.clear();
        idToPhone.clear();
        inMemoryDb.clear();

        List<Person> all = repository.findAll();
        for (Person p : all) {
            if (p.getPhone() != null) {
                phoneIndex.put(p.getPhone(), p);
            }
            if (p.getId() != null && p.getPhone() != null) {
                idToPhone.put(p.getId(), p.getPhone());
            }
            inMemoryDb.add(p);
        }
    }

    // Reload from in-memory only → used in in-memory mode
    public synchronized void reloadInMemory() {
        phoneIndex.clear();
        idToPhone.clear();

        for (Person p : inMemoryDb) {
            if (p.getPhone() != null) {
                phoneIndex.put(p.getPhone(), p);
            }
            if (p.getId() != null && p.getPhone() != null) {
                idToPhone.put(p.getId(), p.getPhone());
            }
        }
    }

    // ===== Dirty flag helpers =====
    private void markDirty() {
        version.incrementAndGet();
    }

    public long getVersion() {
        return version.get();
    }

    // ===== Locking helpers =====
    /*public boolean lockRecord(Integer id, String userId) {
        // Only one user can acquire the lock at a time
        return activeLocks.putIfAbsent(id, userId) == null;
    }

    public void unlockRecord(Integer id, String userId) {
        // Only unlock if the same user is holding the lock
        activeLocks.computeIfPresent(id, (k, v) -> v.equals(userId) ? null : v);
    }

    public Optional<String> getRecordLockOwner(Integer id) {
        return Optional.ofNullable(activeLocks.get(id));
    }*/



    public List<Person> findAll() {
        return repository.findAll();
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
            });

        } else {
            // UPDATE
            String oldPhone = idToPhone.get(id);
            //repository.update(contact);
            boolean updated = repository.update(contact);
            if (!updated) {
                throw new StaleDataException("This record has been updated by another user. Please refresh your data.");
            }

            if (oldPhone != null && !oldPhone.equals(phone)) {
                phoneIndex.remove(oldPhone);
            }
            phoneIndex.put(phone, contact);
            idToPhone.put(id, phone);
        }
        markDirty();
    }

    // Delete contact
    public synchronized void delete(Person contact) {
        if (contact == null) return;

        //repository.delete(contact);
        boolean deleted = repository.delete(contact);
        if (!deleted) {
            throw new StaleDataException("This record has been updated by another user. Please refresh your data.");
        }

        if (contact.getPhone() != null) {
            phoneIndex.remove(contact.getPhone());
        }
        if (contact.getId() != null) {
            idToPhone.remove(contact.getId());
        }
        markDirty();

    }



    public Optional<Person> findById(Integer id) {
        return repository.findById(id);
    }

    public Person getFromCache(String phone) {
        return phoneIndex.get(phone); // O(1)
    }


}
