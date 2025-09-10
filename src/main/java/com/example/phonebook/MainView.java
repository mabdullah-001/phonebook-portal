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
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.component.dialog.Dialog;

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

    // Binder
    private final Binder<Contact> binder = new Binder<>(Contact.class);

    // dialog + current editing state
    private final Dialog formDialog = new Dialog();
    private Contact currentContact = null;    // reference to the contact currently being edited (or new)
    private String originalPhone = null;      // store original phone of contact being edited


    public MainView() {
        this.service = new ContactService(new ContactRepositoryJdbc());

        add(new H1("Phonebook Portal"));

        // Configure Grid
        grid.setColumns("name", "phone", "email", "country", "city", "street");
        grid.setSizeFull();

        Grid.Column<Contact> nameCol    = grid.getColumnByKey("name");
        Grid.Column<Contact> phoneCol   = grid.getColumnByKey("phone");
        Grid.Column<Contact> emailCol   = grid.getColumnByKey("email");
        Grid.Column<Contact> countryCol = grid.getColumnByKey("country");
        Grid.Column<Contact> cityCol    = grid.getColumnByKey("city");
        Grid.Column<Contact> streetCol  = grid.getColumnByKey("street");

        HeaderRow filterRow = grid.appendHeaderRow();

        TextField nameFilter    = createFilter("Name");
        TextField phoneFilter   = createFilter("Phone");
        TextField emailFilter   = createFilter("Email");
        TextField countryFilter = createFilter("Country");
        TextField cityFilter    = createFilter("City");
        TextField streetFilter  = createFilter("Street");

        filterRow.getCell(nameCol).setComponent(nameFilter);
        filterRow.getCell(phoneCol).setComponent(phoneFilter);
        filterRow.getCell(emailCol).setComponent(emailFilter);
        filterRow.getCell(countryCol).setComponent(countryFilter);
        filterRow.getCell(cityCol).setComponent(cityFilter);
        filterRow.getCell(streetCol).setComponent(streetFilter);

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

        nameFilter.addValueChangeListener(e -> applyColumnFilters.run());
        phoneFilter.addValueChangeListener(e -> applyColumnFilters.run());
        emailFilter.addValueChangeListener(e -> applyColumnFilters.run());
        countryFilter.addValueChangeListener(e -> applyColumnFilters.run());
        cityFilter.addValueChangeListener(e -> applyColumnFilters.run());
        streetFilter.addValueChangeListener(e -> applyColumnFilters.run());

        refreshGrid();

        /*grid.asSingleSelect().addValueChangeListener(event -> {
            Contact selected = event.getValue();
            if (selected != null) {
                fillForm(selected);
            }
        });*/

        grid.asSingleSelect().addValueChangeListener(event -> {
            Contact selected = event.getValue();
            if (selected != null) {
                openFormDialog(selected);
            }
        });


        /*// Form Layout
        FormLayout formLayout = new FormLayout(
                nameField, phoneField, emailField, countryField, cityField, streetField
        );
        HorizontalLayout buttons = new HorizontalLayout(saveButton, deleteButton, clearButton);*/

        // --- Binder Setup ---
        binder.forField(nameField)
                .asRequired("Name is required")
                .bind(Contact::getName, Contact::setName);

        binder.forField(phoneField)
                .asRequired("Phone is required")
                .withValidator(phone -> phone.matches("\\d+"), "Phone must contain only digits")
                .withValidator(phone -> {
                    Optional<Contact> existing = service.getContactByPhone(phone);
                    if (existing.isEmpty()) {
                        return true; // no one has this phone → OK
                    }
                    // allow if editing and this phone equals the original phone
                    return originalPhone != null && existing.get().getPhone().equals(originalPhone);
                }, "Phone must be unique")
                .bind(Contact::getPhone, Contact::setPhone);

        binder.forField(emailField)
                .asRequired("Email is required")
                .withValidator(email -> email.contains("@"), "Must be a valid email")
                .bind(Contact::getEmail, Contact::setEmail);

        binder.forField(countryField)
                .asRequired("Country is required")
                .bind(Contact::getCountry, Contact::setCountry);

        binder.forField(cityField)
                .asRequired("City is required")
                .bind(Contact::getCity, Contact::setCity);

        binder.forField(streetField)
                .bind(Contact::getStreet, Contact::setStreet);

        // Enable save only if valid
        binder.addStatusChangeListener(e -> saveButton.setEnabled(binder.isValid()));

        /*// Button actions
        saveButton.addClickListener(e -> saveContact());
        deleteButton.addClickListener(e -> deleteContact());
        clearButton.addClickListener(e -> clearForm());

        add(grid, formLayout, buttons);
        setSizeFull();*/

        // add toolbar button (opens dialog for new contact)
        Button addContactBtn = new Button("Add Contact", e -> openFormDialog(new Contact()));
        //add(addContactBtn, grid);
        add(grid,addContactBtn);
        setSizeFull();

        // configure the popup dialog (form inside a dialog)
        configureFormDialog();

    }

    private void openFormDialog(Contact contact) {
        // set current contact & original phone for edit detection
        currentContact = contact;
        originalPhone = (contact.getPhone() == null || contact.getPhone().isBlank()) ? null : contact.getPhone();

        // bind bean to binder so fields map to this object (live updates)
        binder.setBean(currentContact);

        // enable/disable delete: hide delete when creating new
        deleteButton.setVisible(originalPhone != null);

        // open the dialog
        formDialog.open();
    }

    private void configureFormDialog() {

        // Form layout inside dialog
        FormLayout formLayout = new FormLayout(
                nameField, phoneField, emailField, countryField, cityField, streetField
        );
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 2)  // at all widths → 2 columns
        );

        // Buttons in dialog
        HorizontalLayout buttons = new HorizontalLayout();
        buttons.add(saveButton, deleteButton, clearButton);

        // Save -> validate & persist
        saveButton.addClickListener(e -> {
            saveContact(); // we'll modify saveContact() below to use currentContact/binder
        });

        // Delete -> delete currentContact
        deleteButton.addClickListener(e -> {
            deleteContact(); // we'll modify deleteContact() below
        });

        // Cancel -> close dialog and clear state
        clearButton.setText("Clear");
        clearButton.addClickListener(e -> {
            clearForm();       // clears fields + validation
            formDialog.close();
        });

        formDialog.setHeaderTitle("Add / Edit Contact");
        formDialog.add(formLayout, buttons);
        formDialog.setWidth("70%");
        formDialog.setHeight("60%");
        formDialog.setCloseOnEsc(true);
        formDialog.setCloseOnOutsideClick(false); // avoid accidental close

    }

    private void saveContact() {
        try {
            // Validate form values
            if (!binder.validate().isOk()) {
                Notification.show("Please fix validation errors before saving.");
                return;
            }

            // binder is set to currentContact, so get the bean
            Contact bean = binder.getBean();
            if (bean == null) {
                Notification.show("No contact to save.");
                return;
            }

            // If this is a NEW contact (originalPhone == null) → add
            if (originalPhone == null) {
                service.addContact(bean);
                Notification.show("Contact added: " + bean.getName());
            } else {
                // Editing existing contact:
                // If phone has changed, ensure uniqueness (extra guard)
                if (!originalPhone.equals(bean.getPhone())) {
                    if (service.getContactByPhone(bean.getPhone()).isPresent()) {
                        Notification.show("Phone already exists. Choose a different phone.");
                        return;
                    }
                }
                service.updateContact(bean);
                Notification.show("Contact updated: " + bean.getName());
            }

            refreshGrid();
            formDialog.close();
            // clear currentContact and originalPhone after save
            currentContact = null;
            originalPhone = null;
            binder.setBean(null);
        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage());
        }
    }


    /*private void saveContact() {
        try {
            if (binder.validate().isOk()) {
                Contact contact = new Contact(
                        nameField.getValue(),
                        phoneField.getValue(),
                        emailField.getValue(),
                        countryField.getValue(),
                        cityField.getValue(),
                        streetField.getValue()
                );

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
            }
        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage());
        }
    }*/



    /*private void deleteContact() {
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
    }*/

    private void deleteContact() {
        Contact bean = binder.getBean(); // current bean in dialog
        if (bean == null || bean.getPhone() == null || bean.getPhone().isBlank()) {
            Notification.show("Please select a contact first.");
            return;
        }

        service.getContactByPhone(bean.getPhone()).ifPresent(contact -> {
            service.deleteContact(contact);
            Notification.show("Deleted: " + contact.getName());
            refreshGrid();
            formDialog.close();
        });
    }



    private void clearForm() {
        nameField.clear();
        phoneField.clear();
        emailField.clear();
        countryField.clear();
        cityField.clear();
        streetField.clear();

        binder.readBean(null); // clears validation messages
        currentContact = null;
        originalPhone = null;
    }

    private void fillForm(Contact contact) {
        binder.readBean(contact);
    }

    private void refreshGrid() {
        grid.setItems(service.getAllContacts());
    }

    private TextField createFilter(String placeholder) {
        TextField tf = new TextField();
        tf.setPlaceholder("Filter " + placeholder);
        tf.setClearButtonVisible(true);
        tf.setWidthFull();
        tf.setValueChangeMode(ValueChangeMode.LAZY);
        return tf;
    }

    private boolean contains(String value, String filter) {
        if (filter == null || filter.isBlank()) return true;
        return value != null && value.toLowerCase().contains(filter.toLowerCase());
    }
}
