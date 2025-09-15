package com.example.phonebook;


import com.example.phonebook.lock.Broadcaster;
import com.example.phonebook.lock.LockRegistry;
import com.example.phonebook.model.Person;
import com.example.phonebook.repository.DataService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
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

import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.button.Button;

import com.vaadin.flow.component.icon.VaadinIcon;


import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import java.util.Objects;
import java.util.function.Consumer;


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


    // for push notifications
    private final String sessionId = java.util.UUID.randomUUID().toString();
    private Consumer<String> broadcasterListener;


    public MainView() {
        // tag::snippet[]
        crud = new Crud<>(Person.class, createEditor());
        setupDataProvider();
        setupGrid();

        setupToolbar();

        add(crud);
        // end::snippet[]
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
                    // If field is empty/null, let the required validator handle it (don't report "already exists")
                    if (phone == null || phone.isBlank()) {
                        return true;
                    }

                    // Get the currently edited bean from the binder (may be null in some edge cases)
                    /*Person editing = binder.getBean();
                    Integer editingId = (editing == null) ? null : editing.getId();*/

                    // O(1) cache lookup (doesn't hit DB)
                    Person existing = DataService.getInstance().getFromCache(phone);

                    Person editing = crud.getEditor().getItem();
                    Integer editingId = (editing == null) ? null : editing.getId();


                    // Valid if:
                    //  - there is no Person with this phone (existing == null), OR
                    //  - the existing person is the same record currently being edited
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
            if (person == null || person.getId() == null) {
                return;
            }
            int recordId = person.getId();


            String holderMeta = person.getName() != null ? person.getName() : sessionId;
            boolean acquired = LockRegistry.tryAcquire(recordId, sessionId, holderMeta);
            if (acquired) {
                // we own the lock — open editor
                crud.edit(person, Crud.EditMode.EXISTING_ITEM);
            } else {
                // notify user who holds it (if known)
                LockRegistry.getHolderSessionId(recordId).ifPresentOrElse(holderSid -> {
                    if (!holderSid.equals(sessionId)) {
                        Notification.show("This record is already being edited by another user.", 4000, Notification.Position.MIDDLE);
                    } else {
                        // rare case: we are the holder (maybe re-open)
                        crud.edit(person, Crud.EditMode.EXISTING_ITEM);
                    }
                }, () -> {
                    // no holder info (should not happen), just warn
                    Notification.show("This record is currently locked.", 4000, Notification.Position.MIDDLE);
                });
            }
        });


    }

    private void setupDataProvider() {
        //DataService dataService = new DataService();
        DataService dataService = DataService.getInstance(); // use central singleton
        PersonDataProvider dataProvider = new PersonDataProvider(dataService, true); // true = DB mode, false = in-memory
        crud.setDataProvider(dataProvider);
        /*crud.addDeleteListener(
                deleteEvent -> dataProvider.delete(deleteEvent.getItem()));
        crud.addSaveListener(
                saveEvent -> dataProvider.persist(saveEvent.getItem()));*/

        crud.addSaveListener(saveEvent -> {
            Person saved = saveEvent.getItem();
            // Persist first (so DB is updated). If persist throws, we do not release lock.
            try {
                dataProvider.persist(saved);
                // After successful persist, release the lock for this record
                if (saved.getId() != null) {
                    LockRegistry.release(saved.getId(), sessionId);
                }
            } catch (Exception ex) {
                // show error and keep lock (so user may retry)
                Notification.show("Save failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
                throw ex; // rethrow if you want Crud to handle it
            }
        });

        crud.addDeleteListener(deleteEvent -> {
            Person deleted = deleteEvent.getItem();
            try {
                dataProvider.delete(deleted);
                if (deleted.getId() != null) {
                    LockRegistry.release(deleted.getId(), sessionId);
                }
            } catch (Exception ex) {
                Notification.show("Delete failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
                throw ex;
            }
        });

// Release lock when user cancels editing
        crud.addCancelListener(cancelEvent -> {
            Person cancelled = cancelEvent.getItem();
            if (cancelled != null && cancelled.getId() != null) {
                LockRegistry.release(cancelled.getId(), sessionId);
            }
        });

    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // capture UI reference for safe UI.access() inside listener
        final UI ui = attachEvent.getUI();

        // create and register listener that will be invoked on broadcasts
        broadcasterListener = message -> {
            // Called from broadcaster executor thread — use ui.access to update UI
            ui.access(() -> handleBroadcastMessage(message));
        };
        Broadcaster.register(broadcasterListener);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        // cleanup broadcaster registration
        if (broadcasterListener != null) {
            Broadcaster.unregister(broadcasterListener);
            broadcasterListener = null;
        }
        super.onDetach(detachEvent);
    }

    private void handleBroadcastMessage(String message) {
        try {
            // Expect messages like "LOCK:123:sessionId:metaEncoded" or "UNLOCK:123:sessionId"
            if (message == null || message.isEmpty()) return;
            if ("DATA_UPDATED".equals(message)) {
                // Refresh grid data when someone else makes CRUD changes
                crud.getDataProvider().refreshAll();
                Notification.show("Data updated by another user", 3000, Notification.Position.BOTTOM_START);
                return;
            }
            String[] parts = message.split(":", 4);
            String type = parts[0];
            int recordId = Integer.parseInt(parts[1]);
            String ownerSession = parts[2];
            String meta = parts.length >= 4 ? URLDecoder.decode(parts[3], StandardCharsets.UTF_8) : "";

            if ("LOCK".equals(type)) {
                // someone locked a record. If it's me, we already opened editor.
                if (!sessionId.equals(ownerSession)) {
                    // optional: show small notification to this user
                    // you can refine: only show if this UI is viewing the same record, etc.
                    Notification.show("Record " + recordId + " is now being edited by another user (" + meta + ").", 3000, Notification.Position.MIDDLE);
                }
            } else if ("UNLOCK".equals(type)) {
                if (!sessionId.equals(ownerSession)) {
                    Notification.show("Record " + recordId + " is now available for editing.", 3000, Notification.Position.MIDDLE);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }



}

