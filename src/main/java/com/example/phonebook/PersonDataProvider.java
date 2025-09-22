package com.example.phonebook;

import com.example.phonebook.Database;
import com.example.phonebook.Person;
import com.example.phonebook.StaleDataException;
import com.vaadin.flow.component.crud.CrudFilter;
import com.vaadin.flow.data.provider.AbstractBackEndDataProvider;
import com.vaadin.flow.data.provider.Query;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class PersonDataProvider
        extends AbstractBackEndDataProvider<Person, CrudFilter> {

    // These static fields ensure the in-memory data and cache are shared across all user sessions.
    private static final List<Person> inMemoryDb = new ArrayList<>();
    private static final ConcurrentMap<String, Person> phoneIndex = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Integer, String> idToPhone = new ConcurrentHashMap<>();
    private final boolean useDatabase;

    // Helper methods for unit testing
    public ConcurrentMap<String, Person> getPhoneIndex() {
        return phoneIndex;
    }

    public ConcurrentMap<Integer, String> getIdToPhone() {
        return idToPhone;
    }

    public PersonDataProvider(boolean useDatabase) {
        this.useDatabase = useDatabase;
        if (useDatabase) {
            // In DB mode, populate the cache from the database on startup.
            findAllFromDb().forEach(p -> phoneIndex.put(p.getPhone(), p));
            findAllFromDb().forEach(p -> idToPhone.put(p.getId(), p.getPhone()));
        }
    }

    @Override
    protected Stream<Person> fetchFromBackEnd(Query<Person, CrudFilter> query) {
        Stream<Person> stream;
        if (useDatabase) {
            stream = findAllFromDb().stream();
        } else {
            // In in-memory mode, use the shared static list.
            stream = inMemoryDb.stream();
        }

        if (query.getFilter().isPresent()) {
            stream = stream.filter(predicate(query.getFilter().get()));
        }
        return stream.skip(query.getOffset()).limit(query.getLimit());
    }

    @Override
    protected int sizeInBackEnd(Query<Person, CrudFilter> query) {
        List<Person> allContacts;
        if (useDatabase) {
            allContacts = findAllFromDb();
        } else {
            // In in-memory mode, get size from the shared static list.
            allContacts = inMemoryDb;
        }

        Stream<Person> stream = allContacts.stream();
        if (query.getFilter().isPresent()) {
            stream = stream.filter(predicate(query.getFilter().get()));
        }
        return (int) stream.count();
    }

    public synchronized void persist(Person contact) throws StaleDataException {
        if (useDatabase) {
            if (contact.getId() == null) {
                addPerson(contact);
                phoneIndex.put(contact.getPhone(), contact);
                idToPhone.put(contact.getId(), contact.getPhone());
            } else {
                //String oldPhone = findPhoneById(contact.getId());
                String oldPhone = idToPhone.get(contact.getId());
                updatePerson(contact);
                if (oldPhone != null && !oldPhone.equals(contact.getPhone())) {
                    phoneIndex.remove(oldPhone);
                    idToPhone.remove(contact.getId());
                }
                phoneIndex.put(contact.getPhone(), contact);
                idToPhone.put(contact.getId(), contact.getPhone());
            }
        } else {
            if (contact.getId() == null) {
                contact.setId(inMemoryDb.stream()
                        .map(Person::getId)
                        .filter(Objects::nonNull)
                        .max(Integer::compare)
                        .orElse(0) + 1);
                contact.setLastUpdated(new Date(System.currentTimeMillis()));

                // Always work with a defensive copy
                Person p = new Person(contact);

                inMemoryDb.add(p);
                phoneIndex.put(p.getPhone(), p);
                idToPhone.put(p.getId(), p.getPhone());

            } else {
                Optional<Person> existing = inMemoryDb.stream()
                        .filter(p -> p.getId().equals(contact.getId()))
                        .findFirst();

                if (existing.isPresent()) {
                    int index = inMemoryDb.indexOf(existing.get());

                    if (existing.get().getLastUpdated().equals(contact.getLastUpdated())) {
                        // Update allowed → create new defensive copy
                        Person p = new Person(contact);
                        p.setLastUpdated(new Date(System.currentTimeMillis()));

                        // Replace old object with the new copy
                        inMemoryDb.set(index, p);

                        // Update indexes
                        String oldPhone = idToPhone.get(p.getId());
                        if (oldPhone != null && !oldPhone.equals(p.getPhone())) {
                            phoneIndex.remove(oldPhone);
                        }
                        phoneIndex.put(p.getPhone(), p);
                        idToPhone.put(p.getId(), p.getPhone());

                    } else {
                        throw new StaleDataException(
                                "This record has been updated by another user. Please refresh your data."
                        );
                    }

                } else {
                    // Not found → treat as new record
                    Person p = new Person(contact);
                    p.setLastUpdated(new Date(System.currentTimeMillis()));

                    inMemoryDb.add(p);
                    phoneIndex.put(p.getPhone(), p);
                    idToPhone.put(p.getId(), p.getPhone());
                }
            }
        }
    }

    public synchronized void delete(Person contact) throws StaleDataException {
        if (useDatabase) {
            deletePerson(contact);
            phoneIndex.remove(contact.getPhone());
            idToPhone.remove(contact.getId());
        } else {
            inMemoryDb.removeIf(p -> Objects.equals(p.getId(), contact.getId()));
            phoneIndex.remove(contact.getPhone());
            idToPhone.remove(contact.getId());
        }
    }

    public boolean isPhoneNumberUnique(String phone, Integer currentId) {
        Person existingPerson = phoneIndex.get(phone);
        if (existingPerson != null) {
            return Objects.equals(existingPerson.getId(), currentId);
        }
        return true;
    }

    private List<Person> findAllFromDb() {
        List<Person> contacts = new ArrayList<>();
        String sql = "SELECT id, name, phone, email, country, city, street, last_updated FROM contacts ORDER BY name";
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

    private void addPerson(Person contact) {
        String sql = "INSERT INTO contacts (name, phone, email, country, city, street) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, contact.getName());
            stmt.setString(2, contact.getPhone());
            stmt.setString(3, contact.getEmail());
            stmt.setString(4, contact.getCountry());
            stmt.setString(5, contact.getCity());
            stmt.setString(6, contact.getStreet());
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    contact.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updatePerson(Person contact) throws StaleDataException {
        String sql = "UPDATE contacts SET name=?, phone=?, email=?, country=?, city=?, street=? WHERE id=? AND last_updated=?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, contact.getName());
            stmt.setString(2, contact.getPhone());
            stmt.setString(3, contact.getEmail());
            stmt.setString(4, contact.getCountry());
            stmt.setString(5, contact.getCity());
            stmt.setString(6, contact.getStreet());
            stmt.setInt(7, contact.getId());
            stmt.setTimestamp(8, new Timestamp(contact.getLastUpdated().getTime()));
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new StaleDataException("This record has been updated by another user. Please refresh your data.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deletePerson(Person contact) throws StaleDataException {
        String sql = "DELETE FROM contacts WHERE id=? AND last_updated=?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, contact.getId());
            stmt.setTimestamp(2, new Timestamp(contact.getLastUpdated().getTime()));
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new StaleDataException("This record has been updated by another user. Please refresh your data.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String findPhoneById(Integer id) {
        String sql = "SELECT phone FROM contacts WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("phone");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
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
        person.setLastUpdated(rs.getTimestamp("last_updated"));
        return person;
    }

    private static Predicate<Person> predicate(CrudFilter filter) {
        return filter.getConstraints().entrySet().stream()
                .map(constraint -> (Predicate<Person>) person -> {
                    try {
                        Object value = valueOf(constraint.getKey(), person);
                        return value != null && value.toString().toLowerCase()
                                .contains(constraint.getValue().toLowerCase());
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                })
                .reduce(Predicate::and)
                .orElse(person -> true);
    }

    private static Object valueOf(String fieldName, Person person) {
        try {
            return Person.class.getDeclaredField(fieldName).get(person);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}//eof