package com.example.phonebook;


import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.example.phonebook.model.Person;
import com.example.phonebook.repository.DataService;
import com.example.phonebook.repository.exception.StaleDataException;
import com.vaadin.flow.component.crud.CrudFilter;
import com.vaadin.flow.data.provider.AbstractBackEndDataProvider;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.provider.SortDirection;
import static java.util.Comparator.naturalOrder;

public class PersonDataProvider
        extends AbstractBackEndDataProvider<Person, CrudFilter> {

    // A real app should hook up something like JPA
    //private final List<Person> DATABASE = new ArrayList<>();
    private final DataService dataService;
    private final boolean useDatabase;
    private Consumer<Long> sizeChangeListener;


    public PersonDataProvider(DataService dataService, boolean useDatabase) {
        this.dataService = dataService;
        this.useDatabase = useDatabase;
    }










    @Override
    protected Stream<Person> fetchFromBackEnd(Query<Person, CrudFilter> query) {
        int offset = query.getOffset();
        int limit = query.getLimit();

        if (useDatabase) {
            dataService.reloadFromDatabase();
        } else {
            // Always rebuild indexes from in-memory list, no DB hit
            dataService.reloadInMemory();
        }

        Stream<Person> stream = dataService.getInMemory().stream();

        if (query.getFilter().isPresent()) {
            stream = stream.filter(predicate(query.getFilter().get()))
                    .sorted(comparator(query.getFilter().get()));
        }

        return stream.skip(offset).limit(limit);
    }

    @Override
    protected int sizeInBackEnd(Query<Person, CrudFilter> query) {
        // Just apply same logic on DATABASE
        Stream<Person> stream = fetchFromBackEnd(query);

        if (query.getFilter().isPresent()) {
            stream = stream.filter(predicate(query.getFilter().get()));
        }

        long count = stream.count();

        if (sizeChangeListener != null) {
            sizeChangeListener.accept(count);
        }

        return (int) count;
    }



    private static Predicate<Person> predicate(CrudFilter filter) {
        // For RDBMS just generate a WHERE clause
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
                }).reduce(Predicate::and).orElse(e -> true);
    }

    private static Comparator<Person> comparator(CrudFilter filter) {
        // For RDBMS just generate an ORDER BY clause
        return filter.getSortOrders().entrySet().stream().map(sortClause -> {
            try {
                Comparator<Person> comparator = Comparator.comparing(
                        person -> (Comparable) valueOf(sortClause.getKey(),
                                person));

                if (sortClause.getValue() == SortDirection.DESCENDING) {
                    comparator = comparator.reversed();
                }

                return comparator;

            } catch (Exception ex) {
                return (Comparator<Person>) (o1, o2) -> 0;
            }
        }).reduce(Comparator::thenComparing).orElse((o1, o2) -> 0);
    }

    private static Object valueOf(String fieldName, Person person) {
        try {
            Field field = Person.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(person);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    void persist(Person item) {
        if (useDatabase) {
            try {
                dataService.save(item);
                refreshItem(item);
            } catch (StaleDataException e) {
                // Propagate the exception to MainView
                throw e;
            }
        } else {
            List<Person> mem = dataService.getInMemory();
            if (item.getId() == null) {
                item.setId(mem.stream().map(Person::getId)
                        .max(naturalOrder()).orElse(0) + 1);
            }
            final Optional<Person> existingItem = mem.stream()
                    .filter(p -> p.getId().equals(item.getId()))
                    .findFirst();

            if (existingItem.isPresent()) {
                int position = mem.indexOf(existingItem.get());
                mem.remove(existingItem.get());
                mem.add(position, item);
            } else {
                mem.add(item);
            }
            dataService.reloadInMemory();  // rebuild indexes
        }
    }



    /*Optional<Person> find(Integer id) {
        if (useDatabase) {
            return dataService.findById(id);
        } else {
            return DATABASE.stream().filter(entity -> entity.getId().equals(id)).findFirst();
        }
    }*/


    void delete(Person item) {
        if (useDatabase) {
            try {
                dataService.delete(item);
            } catch (StaleDataException e) {
                // Propagate the exception to MainView
                throw e;
            }
        } else {
            List<Person> mem = dataService.getInMemory();
            mem.removeIf(entity -> entity.getId().equals(item.getId()));
            dataService.reloadInMemory();  // rebuild indexes
        }
    }
}// end of class
