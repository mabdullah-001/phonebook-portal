package com.example.phonebook;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.crud.BinderCrudEditor;
import com.vaadin.flow.component.crud.Crud;
import com.vaadin.flow.component.crud.CrudEditor;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Route;

import java.util.Arrays;
import java.util.List;


@Route("")
public class MainView extends Div {

    private Crud<Person> crud;
    private final PersonDataProvider dataProvider;
    private static final String NAME = "name";
    private static final String PHONE_NUMBER = "phone";
    private static final String EMAIL = "email";
    private static final String STREET = "street";
    private static final String CITY = "city";
    private static final String COUNTRY = "country";
    private static final String EDIT_COLUMN = "vaadin-crud-edit-column";

    // Flag to switch between database and in-memory storage
    private final boolean useDatabase = false;


    public MainView() {
        this.dataProvider = new PersonDataProvider(useDatabase);
        this.crud = new Crud<>(Person.class, createEditor());
        setupDataProvider();
        setupGrid();
        setupToolbar();
        add(crud);
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

        FormLayout form = new FormLayout(name, phoneNumber, email, street, city, country);

        Binder<Person> binder = new Binder<>(Person.class);
        binder.forField(name).asRequired().bind(Person::getName, Person::setName);

        binder.forField(phoneNumber)
                .asRequired("Phone is required")
                .withValidator(phone -> phone != null && phone.matches("\\d+"), "Phone must contain only digits")
                .withValidator(phone -> {
                    if (phone == null || phone.isBlank()) {
                        return true;
                    }
                    Person editing = crud.getEditor().getItem();
                    Integer editingId = (editing == null) ? null : editing.getId();
                    return dataProvider.isPhoneNumberUnique(phone, editingId);
                }, "Phone Number already exists")
                .bind(Person::getPhone, Person::setPhone);

        binder.forField(email).asRequired()
                .withValidator(
                        value -> value != null && value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"),
                        "Invalid email address")
                .bind(Person::getEmail, Person::setEmail);
        binder.forField(street).asRequired().bind(Person::getStreet, Person::setStreet);
        binder.forField(city).asRequired().bind(Person::getCity, Person::setCity);
        binder.forField(country).asRequired().bind(Person::getCountry, Person::setCountry);

        return new BinderCrudEditor<>(binder, form);
    }

    private void setupGrid() {
        Grid<Person> grid = crud.getGrid();

        List<String> visibleColumns = Arrays.asList(NAME, PHONE_NUMBER,
                EMAIL, COUNTRY, EDIT_COLUMN);
        grid.getColumns().forEach(column -> {
            String key = column.getKey();
            if (!visibleColumns.contains(key)) {
                grid.removeColumn(column);
            }
        });

        // Reorder the columns (alphabetical by default)
        grid.setColumnOrder(grid.getColumnByKey(NAME),
                grid.getColumnByKey(PHONE_NUMBER), grid.getColumnByKey(EMAIL),
                grid.getColumnByKey(COUNTRY),
                grid.getColumnByKey(EDIT_COLUMN));

        Crud.removeEditColumn(grid);

        grid.addItemDoubleClickListener(event -> crud.edit(event.getItem(), Crud.EditMode.EXISTING_ITEM));
    }


    private void setupDataProvider() {
        crud.setDataProvider(dataProvider);

        crud.addSaveListener(saveEvent -> {
            try {
                dataProvider.persist(saveEvent.getItem());
                crud.getGrid().getDataProvider().refreshAll();
            } catch (StaleDataException e) {
                Notification notification = Notification.show(e.getMessage(), 5000, Notification.Position.BOTTOM_END);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        crud.addEditListener(saveEvent -> {
            try {
                dataProvider.persist(saveEvent.getItem());
                crud.getGrid().getDataProvider().refreshItem(saveEvent.getItem());
            } catch (StaleDataException e) {
                Notification notification = Notification.show(e.getMessage(), 5000, Notification.Position.BOTTOM_END);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        crud.addDeleteListener(deleteEvent -> {
            try {
                dataProvider.delete(deleteEvent.getItem());
                crud.getGrid().getDataProvider().refreshAll();
            } catch (StaleDataException e) {
                Notification notification = Notification.show(e.getMessage(), 5000, Notification.Position.BOTTOM_END);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
    }
}
