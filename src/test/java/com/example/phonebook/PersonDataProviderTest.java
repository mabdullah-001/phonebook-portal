package com.example.phonebook;

import com.vaadin.flow.data.provider.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class PersonDataProviderTest {

    private PersonDataProvider dataProvider;
    private MockedStatic<Database> mockedDatabase;

    @BeforeEach
    void setup() throws Exception {
        // Mock static Database before creating PersonDataProvider
        mockedDatabase = Mockito.mockStatic(Database.class);

        Connection mockConn = Mockito.mock(Connection.class);
        PreparedStatement mockStmt = Mockito.mock(PreparedStatement.class);
        ResultSet mockRs = Mockito.mock(ResultSet.class);

        // Default stubbing for constructor call to findAllFromDb()
        mockedDatabase.when(Database::getConnection).thenReturn(mockConn);
        when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);
        when(mockStmt.executeQuery()).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(false); // no rows at startup

        dataProvider = new PersonDataProvider(true);

        // Clear caches for isolation
        dataProvider.getPhoneIndex().clear();
        dataProvider.getIdToPhone().clear();
    }

    @AfterEach
    void teardown() {
        mockedDatabase.close(); // release static mock
    }

    @Test
    void testCreateNewPersonWithDatabase() throws Exception {
        Person newPerson = new Person();
        newPerson.setName("Test Create");
        newPerson.setPhone("1111111111");

        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        ResultSet mockKeys = mock(ResultSet.class);

        mockedDatabase.when(Database::getConnection).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(mockStatement);
        when(mockStatement.executeUpdate()).thenReturn(1);
        when(mockStatement.getGeneratedKeys()).thenReturn(mockKeys);
        when(mockKeys.next()).thenReturn(true);
        when(mockKeys.getInt(1)).thenReturn(101);

        dataProvider.persist(newPerson);

        verify(mockStatement, times(1)).executeUpdate();
        assertEquals(101, newPerson.getId());
    }

    @Test
    void testReadPersonsFromDatabase() throws Exception {
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStatement = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);

        mockedDatabase.when(Database::getConnection).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);

        // Two rows in the result set
        when(mockResultSet.next()).thenReturn(true, true, false);
        when(mockResultSet.getInt("id")).thenReturn(1, 2);
        when(mockResultSet.getString("name")).thenReturn("Read Test 1", "Read Test 2");
        when(mockResultSet.getString("phone")).thenReturn("123", "456");

        List<Person> fetchedPersons =
                dataProvider.fetchFromBackEnd(new Query<>()).collect(Collectors.toList());

        assertEquals(2, fetchedPersons.size());
        assertEquals("Read Test 1", fetchedPersons.get(0).getName());
        assertEquals("Read Test 2", fetchedPersons.get(1).getName());
    }

    @Test
    void testUpdateExistingPersonWithDatabase() throws Exception {
        Person existingPerson = new Person();
        existingPerson.setId(10);
        existingPerson.setName("Old Name");
        existingPerson.setPhone("9999999999");
        existingPerson.setLastUpdated(new Date());

        dataProvider.getPhoneIndex().put(existingPerson.getPhone(), existingPerson);
        dataProvider.getIdToPhone().put(existingPerson.getId(), existingPerson.getPhone());

        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStatement = mock(PreparedStatement.class);

        mockedDatabase.when(Database::getConnection).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeUpdate()).thenReturn(1);

        Person updatedPerson = new Person(existingPerson);
        updatedPerson.setName("New Name");

        dataProvider.persist(updatedPerson);

        verify(mockStatement, times(1)).executeUpdate();
    }

    @Test
    void testDeletePersonWithDatabase() throws Exception {
        Person personToDelete = new Person();
        personToDelete.setId(20);
        personToDelete.setPhone("8888888888");
        personToDelete.setLastUpdated(new Date());

        dataProvider.getPhoneIndex().put("8888888888", personToDelete);
        dataProvider.getIdToPhone().put(20, "8888888888");

        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockStatement = mock(PreparedStatement.class);

        mockedDatabase.when(Database::getConnection).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);
        when(mockStatement.executeUpdate()).thenReturn(1);

        dataProvider.delete(personToDelete);

        verify(mockStatement, times(1)).executeUpdate();
        assertTrue(dataProvider.getPhoneIndex().isEmpty());
        assertTrue(dataProvider.getIdToPhone().isEmpty());
    }
}
