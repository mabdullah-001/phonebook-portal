# 📒 Phonebook Portal (Vaadin + Jetty + MySQL)

A simple **Phonebook Management Portal** built with **Vaadin (no Spring)**, running on **Jetty** with **MySQL database integration**.  
The application supports **CRUD operations (Create, Read, Update, Delete)**, **search filters**, and **form validation**.

---

##  Features

- **Contact Management**
    - Add, edit, and delete contacts.
    - Fields: Name, Phone (unique), Email, Country, City, Street.
- **Search & Filters**
    - Search across all columns with per-column filters.
- **Validation**
    - Required fields enforced (Name, Phone).
    - Phone must be numeric and unique.
    - Email format validated.
- **UI**
    - Grid for listing contacts.
    - Responsive popup form for editing.
    - Inline validation messages.
    - Delete confirmation dialog.
- **Persistence**
    - In-memory repository (for quick testing).
    - JDBC repository with **MySQL database**.

---

## 🛠️ Tech Stack

- **Frontend + Backend**: [Vaadin Flow 24](https://vaadin.com/docs/latest/)
- **Server**: Jetty
- **Database**: MySQL
- **Build Tool**: Maven
- **IDE**: IntelliJ IDEA (recommended)

---

## ⚙️ Setup Instructions

### 1. Prerequisites
- Install **Java 17** (required for Vaadin 24).
- Install **Maven**.
- Install **MySQL Server** + **MySQL Workbench**.
- (Optional) IntelliJ IDEA with the **Database Tools** plugin.

---

### 2. Database Setup

Run the following in **MySQL Workbench**:

```sql
CREATE DATABASE phonebook;

USE phonebook;

CREATE TABLE contacts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(100),
    country VARCHAR(50),
    city VARCHAR(50),
    street VARCHAR(100)
);
