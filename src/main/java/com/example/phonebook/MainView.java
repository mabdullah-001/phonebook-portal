package com.example.phonebook;


import com.example.phonebook.model.Person;
import com.example.phonebook.repository.DataService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudEditor;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;


import com.vaadin.flow.component.notification.Notification;

import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;

import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.button.Button;

import com.vaadin.flow.component.icon.VaadinIcon;



import java.util.Arrays;
import java.util.List;

import java.util.Objects;


@Route("")
public class MainView extends Div {

    private Crud<Person> crud;

    private String NAME = "name";
    private String PHONE_NUMBER= "phone";
    private String EMAIL = "email";
    private String STREET = "street";
    private String CITY = "city";
    private String COUNTRY = "country";
    private String EDIT_COLUMN = "vaadin-crud-edit-column";

    // Unique user/session ID
    private final String userId = UI.getCurrent().getSession().getSession().getId();
    private boolean lastUpdateByMe = false;

    public MainView() {
        // tag::snippet[]
        crud = new Crud<>(Person.class, createEditor());
        setupDataProvider();
        setupGrid();

        setupToolbar();

        add(crud);

        setupAutoRefresh(crud);
    }

    private void setupAutoRefresh(Crud<Person> crud) {
        // Track last seen version for this UI
        final long[] lastSeenVersion = { DataService.getInstance().getVersion() };

        // Enable Vaadin polling (every 3 seconds)
        UI.getCurrent().setPollInterval(3000);

        UI.getCurrent().addPollListener(e -> {
            long currentVersion = DataService.getInstance().getVersion();
            if (currentVersion != lastSeenVersion[0]) {
                lastSeenVersion[0] = currentVersion;
                crud.getGrid().getDataProvider().refreshAll();
                if (lastUpdateByMe) {
                    lastUpdateByMe = false; // reset, no Notification for self
                } else {
                    Notification.show("Data updated from another user",
                            2000, Notification.Position.TOP_END);
                }
            }
        });
    }

    private void setupToolbar() {
        Button button = new Button("Add Contact", VaadinIcon.PLUS.create());
        button.addClickListener(event -> {
            crud.edit(new Person(), Crud.EditMode.NEW_ITEM);
        });
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        crud.setNewButton(button);
    }

    private CrudEditor<Person> createEditor() {
        TextField name = new TextField("name");
        TextField phoneNumber = new TextField("Phone Number");
        EmailField email = new EmailField("Email");
        TextField street = new TextField("Street");
        TextField city = new TextField("City");
        TextField country = new TextField("Country");

        FormLayout form = new FormLayout(name, phoneNumber, email,
                street,city,country);



        DataService dataService = DataService.getInstance();
        Binder<Person> binder = new Binder<>(Person.class);
        binder.forField(name).asRequired().bind(Person::getName,
                Person::setName);
        binder.forField(phoneNumber)
                .asRequired("Phone is required")
                .withValidator(phone -> phone != null && phone.matches("\\d+"), "Phone must contain only digits")
                .withValidator(phone -> {
                    if (phone == null || phone.isBlank()) {
                        return true;
                    }
                    Person existing = DataService.getInstance().getFromCache(phone);
                    Person editing = crud.getEditor().getItem();
                    Integer editingId = (editing == null) ? null : editing.getId();

                    return (existing == null) || Objects.equals(existing.getId(), editingId);
                }, "Phone Number already exists")
                .bind(Person::getPhone, Person::setPhone);

        binder.forField(email).asRequired()
                .withValidator(
                        value -> value != null && value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"),
                        "Invalid email address")
                .bind(Person::getEmail,
                        Person::setEmail);
        binder.forField(street).asRequired().bind(Person::getStreet,
                Person::setStreet);
        binder.forField(city).asRequired().bind(Person::getCity,
                Person::setCity);
        binder.forField(country).asRequired().bind(Person::getCountry,
                Person::setCountry);


        return new BinderCrudEditor<>(binder, form);
    }

    private void setupGrid() {
        Grid<Person> grid = crud.getGrid();

        // Only show these columns (all columns shown by default):
        List<String> visibleColumns = Arrays.asList(NAME, PHONE_NUMBER,
                EMAIL, STREET,CITY,COUNTRY, EDIT_COLUMN);
        grid.getColumns().forEach(column -> {
            String key = column.getKey();
            if (!visibleColumns.contains(key)) {
                grid.removeColumn(column);
            }
        });
        /*grid.getColumns().forEach(col ->
                System.out.println("Column key: " + col.getKey() + ", header: " + col.getHeaderText())
        );*/

        // Reorder the columns (alphabetical by default)
        grid.setColumnOrder(grid.getColumnByKey(NAME),
                grid.getColumnByKey(PHONE_NUMBER), grid.getColumnByKey(EMAIL),
                grid.getColumnByKey(STREET),grid.getColumnByKey(CITY),
                grid.getColumnByKey(COUNTRY),
                grid.getColumnByKey(EDIT_COLUMN));

        Crud.removeEditColumn(grid);
        // grid.removeColumnByKey(EDIT_COLUMN);
        // grid.removeColumn(grid.getColumnByKey(EDIT_COLUMN));

        // Open editor on double click
        /*grid.addItemDoubleClickListener(event -> crud.edit(event.getItem(),
                Crud.EditMode.EXISTING_ITEM));*/
        grid.addItemDoubleClickListener(event -> {
            Person person = event.getItem();
            boolean locked = DataService.getInstance().lockRecord(person.getId(), userId);

            if (!locked) {
                Notification.show("This record is already being edited by another user");
            } else {
                crud.edit(person, Crud.EditMode.EXISTING_ITEM);
            }
        });


    }

    private void setupDataProvider() {
        DataService dataService = DataService.getInstance(); // use central singleton
        PersonDataProvider dataProvider = new PersonDataProvider(dataService, false); // true = DB mode, false = in-memory
        crud.setDataProvider(dataProvider);

        crud.addSaveListener(saveEvent -> {
            lastUpdateByMe = true;  // mark change came from me
            Person person = saveEvent.getItem();
            dataProvider.persist(person);
            DataService.getInstance().unlockRecord(person.getId(), userId);
        });

        crud.addDeleteListener(deleteEvent -> {
            lastUpdateByMe = true;  // mark change came from me
            Person person = deleteEvent.getItem();
            dataProvider.delete(person);
            DataService.getInstance().unlockRecord(person.getId(), userId);
        });


        crud.addCancelListener(cancelEvent -> {
            Person person = cancelEvent.getItem();
            if (person != null && person.getId() != null) {
                DataService.getInstance().unlockRecord(person.getId(), userId);
            }
        });


    }




}// end of class

