package com.example.phonebook;

import com.example.phonebook.model.Contact;
import com.example.phonebook.repository.ContactRepositoryJdbc;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.data.value.ValueChangeMode;

import java.util.Optional;

@Route("")
public class MainView extends VerticalLayout {

    private final ContactService service;
    private final Grid<Contact> grid = new Grid<>(Contact.class);


    // Form fields
    private final TextField nameField = new TextField("Name");
    private final TextField phoneField = new TextField("Phone");
    private final EmailField emailField = new EmailField("Email");
    private final TextField countryField = new TextField("Country");
    private final TextField cityField = new TextField("City");
    private final TextField streetField = new TextField("Street");

    private final Button saveButton = new Button("Save");
    private final Button deleteButton = new Button("Delete");
    private final Button clearButton = new Button("Clear");

    public MainView() {
        //this.service = new ContactService(new ContactRepository());// for in memory storage
        this.service = new ContactService(new ContactRepositoryJdbc());// for My sql db storage

        // Title
        add(new H1("Phonebook Portal"));



        // Configure Grid
        grid.setColumns("name", "phone", "email", "country", "city", "street");
        grid.setSizeFull();
        // Add filter row
        Grid.Column<Contact> nameCol    = grid.getColumnByKey("name");
        Grid.Column<Contact> phoneCol   = grid.getColumnByKey("phone");
        Grid.Column<Contact> emailCol   = grid.getColumnByKey("email");
        Grid.Column<Contact> countryCol = grid.getColumnByKey("country");
        Grid.Column<Contact> cityCol    = grid.getColumnByKey("city");
        Grid.Column<Contact> streetCol  = grid.getColumnByKey("street");

        HeaderRow filterRow = grid.appendHeaderRow();

        // Keep references to filters
        TextField nameFilter    = createFilter("Name");
        TextField phoneFilter   = createFilter("Phone");
        TextField emailFilter   = createFilter("Email");
        TextField countryFilter = createFilter("Country");
        TextField cityFilter    = createFilter("City");
        TextField streetFilter  = createFilter("Street");

        // Put filters into the filter row cells
        filterRow.getCell(nameCol).setComponent(nameFilter);
        filterRow.getCell(phoneCol).setComponent(phoneFilter);
        filterRow.getCell(emailCol).setComponent(emailFilter);
        filterRow.getCell(countryCol).setComponent(countryFilter);
        filterRow.getCell(cityCol).setComponent(cityFilter);
        filterRow.getCell(streetCol).setComponent(streetFilter);

        // Filtering logic
        // service.getAllContacts() will get all contacts sorted by name
        // then stream transforms it into a processing pipeline.
        // .filter removes contacts that do not match the corresponding column filter.
        // .toList() collects the remaining items into a List
        // and then finally grid.setItems(...) replaces the grid contents with the filtered list.
        Runnable applyColumnFilters = () -> grid.setItems(
                service.getAllContacts().stream()
                        .filter(c -> contains(c.getName(),    nameFilter.getValue()))
                        .filter(c -> contains(c.getPhone(),   phoneFilter.getValue()))
                        .filter(c -> contains(c.getEmail(),   emailFilter.getValue()))
                        .filter(c -> contains(c.getCountry(), countryFilter.getValue()))
                        .filter(c -> contains(c.getCity(),    cityFilter.getValue()))
                        .filter(c -> contains(c.getStreet(),  streetFilter.getValue()))
                        .toList()
        );

        // trigger filter when any field changes
        nameFilter.addValueChangeListener(e -> applyColumnFilters.run());
        phoneFilter.addValueChangeListener(e -> applyColumnFilters.run());
        emailFilter.addValueChangeListener(e -> applyColumnFilters.run());
        countryFilter.addValueChangeListener(e -> applyColumnFilters.run());
        cityFilter.addValueChangeListener(e -> applyColumnFilters.run());
        streetFilter.addValueChangeListener(e -> applyColumnFilters.run());


        // refreshing grid
        refreshGrid();

        // When a row is selected → populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            Contact selected = event.getValue();
            if (selected != null) {
                fillForm(selected);
            }
        });

        // Form Layout
        FormLayout formLayout = new FormLayout(
                nameField, phoneField, emailField, countryField, cityField, streetField
        );
        HorizontalLayout buttons = new HorizontalLayout(saveButton, deleteButton, clearButton);

        // Button actions
        saveButton.addClickListener(e -> saveContact());
        deleteButton.addClickListener(e -> deleteContact());
        clearButton.addClickListener(e -> clearForm());

        // Layout
        add(grid, formLayout, buttons);
        setSizeFull();
    }

    private void saveContact() {
        try {
            Contact contact = new Contact(
                    nameField.getValue(),
                    phoneField.getValue(),
                    emailField.getValue(),
                    countryField.getValue(),
                    cityField.getValue(),
                    streetField.getValue()
            );

            // If phone already exists → update, otherwise add
            Optional<Contact> existing = service.getContactByPhone(contact.getPhone());
            if (existing.isPresent()) {
                service.updateContact(contact);
                Notification.show("Contact updated: " + contact.getName());
            } else {
                service.addContact(contact);
                Notification.show("Contact added: " + contact.getName());
            }

            refreshGrid();
            clearForm();
        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage());
        }
    }

    private void deleteContact() {
        String phone = phoneField.getValue();
        if (phone.isEmpty()) {
            Notification.show("Please select a contact first.");
            return;
        }

        service.getContactByPhone(phone).ifPresent(contact -> {
            service.deleteContact(contact);
            Notification.show("Deleted: " + contact.getName());
            refreshGrid();
            clearForm();
        });
    }

    private void clearForm() {
        nameField.clear();
        phoneField.clear();
        emailField.clear();
        countryField.clear();
        cityField.clear();
        streetField.clear();
    }

    private void fillForm(Contact contact) {
        nameField.setValue(contact.getName());
        phoneField.setValue(contact.getPhone());
        emailField.setValue(contact.getEmail() != null ? contact.getEmail() : "");
        countryField.setValue(contact.getCountry() != null ? contact.getCountry() : "");
        cityField.setValue(contact.getCity() != null ? contact.getCity() : "");
        streetField.setValue(contact.getStreet() != null ? contact.getStreet() : "");
    }



    private void refreshGrid() {
        grid.setItems(service.getAllContacts());
    }

    /* --- helpers --- */
    private TextField createFilter(String placeholder) {
        TextField tf = new TextField();
        tf.setPlaceholder("Filter " + placeholder);
        tf.setClearButtonVisible(true);// cancel button in filter
        tf.setWidthFull();
        tf.setValueChangeMode(ValueChangeMode.LAZY); // fires after user pauses typing
        return tf;
    }

    private boolean contains(String value, String filter) {
        if (filter == null || filter.isBlank()) return true;       // no filter => pass
        return value != null && value.toLowerCase().contains(filter.toLowerCase());
    }
}
