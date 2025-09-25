package com.example.phonebook;

import com.vaadin.flow.component.crud.CrudFilter;
import com.vaadin.flow.data.provider.AbstractBackEndDataProvider;
import com.vaadin.flow.data.provider.Query;

import java.sql.*;
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

    private static final List<Person> inMemoryDb = new ArrayList<>();
    private static final ConcurrentMap<String, Person> phoneIndex = new ConcurrentHashMap<>();
    private final boolean useDatabase;

    // Helper method for unit testing
    public ConcurrentMap<String, Person> getPhoneIndex() {
        return phoneIndex;
    }

    public PersonDataProvider(boolean useDatabase) {
        this.useDatabase = useDatabase;
        if (useDatabase) {
            findAllFromDb().forEach(p -> phoneIndex.put(p.getPhone(), p));
        }
    }

    @Override
    protected Stream<Person> fetchFromBackEnd(Query<Person, CrudFilter> query) {
        Stream<Person> stream = useDatabase ? findAllFromDb().stream() : inMemoryDb.stream();
        if (query.getFilter().isPresent()) {
            stream = stream.filter(predicate(query.getFilter().get()));
        }
        return stream.skip(query.getOffset()).limit(query.getLimit());
    }

    @Override
    protected int sizeInBackEnd(Query<Person, CrudFilter> query) {
        List<Person> allContacts = useDatabase ? findAllFromDb() : inMemoryDb;
        Stream<Person> stream = allContacts.stream();
        if (query.getFilter().isPresent()) {
            stream = stream.filter(predicate(query.getFilter().get()));
        }
        return (int) stream.count();
    }

    public synchronized void persist(Person contact) throws StaleDataException {
        if (contact.getId() == null) {
            persistNew(contact);
        } else {
            persistExisting(contact);
        }
    }

    private void persistNew(Person contact) {
        if (useDatabase) {
            addPerson(contact);
        } else {
            contact.setId(inMemoryDb.stream()
                    .map(Person::getId)
                    .filter(Objects::nonNull)
                    .max(Integer::compare)
                    .orElse(0) + 1);
            contact.setLastUpdated(new Date(System.currentTimeMillis()));

            inMemoryDb.add(new Person(contact));
        }
        phoneIndex.put(contact.getPhone(), contact);
    }

    private void persistExisting(Person contact) throws StaleDataException {
        Person existing = findById(contact.getId());
        if (existing == null) {
            throw new IllegalStateException("No existing record found with id=" + contact.getId());
        }

        if (!Objects.equals(existing.getLastUpdated(), contact.getLastUpdated())) {
            throw new StaleDataException(
                    "This record has been modified by another user. Please reload the data."
            );
        }

        if (!hasContentChanged(existing, contact)) {
            return;
        }

        if (useDatabase) {
            updatePerson(contact);
        } else {
            int index = inMemoryDb.indexOf(existing);
            Person p = new Person(contact);
            p.setLastUpdated(new Date(System.currentTimeMillis()));
            inMemoryDb.set(index, p);
        }


        String oldPhone = existing.getPhone();
        if (!Objects.equals(oldPhone, contact.getPhone())) {
            phoneIndex.remove(oldPhone);
        }
        if (useDatabase) {
            phoneIndex.put(contact.getPhone(), contact);
        } else {
            Person stored = inMemoryDb.stream()
                    .filter(p -> Objects.equals(p.getId(), contact.getId()))
                    .findFirst()
                    .orElse(new Person(contact));
            phoneIndex.put(stored.getPhone(), stored);
        }
    }

    private boolean hasContentChanged(Person existing, Person updated) {
        return !Objects.equals(existing.getName(), updated.getName())
                || !Objects.equals(existing.getPhone(), updated.getPhone())
                || !Objects.equals(existing.getEmail(), updated.getEmail())
                || !Objects.equals(existing.getCountry(), updated.getCountry())
                || !Objects.equals(existing.getCity(), updated.getCity())
                || !Objects.equals(existing.getStreet(), updated.getStreet());
    }



    public synchronized void delete(Person contact) throws StaleDataException {
        if (useDatabase) {
            Person existing = findById(contact.getId());
            if (existing != null) {
                deletePerson(existing);
                phoneIndex.remove(existing.getPhone());
            }
        } else {
            inMemoryDb.removeIf(p -> Objects.equals(p.getId(), contact.getId()));
            phoneIndex.remove(contact.getPhone());
        }
    }

    public boolean isPhoneNumberUnique(String phone, Integer currentId) {
        Person existingPerson = phoneIndex.get(phone);
        if (existingPerson != null) {
            return Objects.equals(existingPerson.getId(), currentId);
        }
        return true;
    }
    public Person findById(Integer id) {
        return phoneIndex.values().stream()
                .filter(p -> Objects.equals(p.getId(), id))
                .findFirst()
                .orElse(null);
    }

    public boolean hasChanged(Person existing, Person updated) {
        return !Objects.equals(existing.getLastUpdated(), updated.getLastUpdated());
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
                contact.setLastUpdated(new Date(System.currentTimeMillis()));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void updatePerson(Person contact) throws StaleDataException {
        String sql = "UPDATE contacts SET name=?, phone=?, email=?, country=?, city=?, street=? " +
                "WHERE id=? AND last_updated=?";
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
                throw new StaleDataException("This record has been modified by another user. Please reload the data.");
            }
            contact.setLastUpdated(new Date(System.currentTimeMillis()));
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
                throw new StaleDataException("This record has been modified by another user. Please ensure you are viewing the latest data.");
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
            var field = Person.class.getDeclaredField(fieldName);
            field.setAccessible(true); // allow reflective access
            return field.get(person);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}