# FSAD-Project-25-Backend
# ⚙️ Online Assignment Submission System – Backend

## 📌 Project Overview

This is the backend of the **Online Assignment Submission and Grading System** built using Spring Boot.
It handles authentication, database operations, and API services.

---

## 🚀 Features

* 🔐 User Authentication (Login/Register)
* 📚 Assignment Management (CRUD)
* 📤 Assignment Submission
* 🔗 REST API Integration
* 🛡️ Exception Handling
* 💾 MySQL Database Integration

---

## 🛠️ Tech Stack

* Java
* Spring Boot
* Spring Data JPA
* MySQL
* Maven

---

## 📂 Project Structure

src/main/java/
├── controller/
├── service/
├── repository/
├── model/
├── config/

---

## 🗄️ Database Configuration

Update `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/assignment_db
spring.datasource.username=root
spring.datasource.password=yourpassword
spring.jpa.hibernate.ddl-auto=update
```

---

## ▶️ Run the Application

```bash
git clone https://github.com/your-username/backend-repo.git
cd backend-repo
mvn spring-boot:run
```

---

## 🔗 API Endpoints

| Method | Endpoint         | Description         |
| ------ | ---------------- | ------------------- |
| POST   | /api/register    | Register user       |
| POST   | /api/login       | Login               |
| GET    | /api/assignments | Get all assignments |
| POST   | /api/assignments | Create assignment   |
| POST   | /api/submissions | Submit assignment   |

---

## 🧪 Testing

You can test APIs using:

* Postman
* Browser (for GET requests)

---

## 👥 Team Contribution

* Backend Development: [Vinay Bhargav Reddy]
* Database Design: [Vinay Bhargav Reddy]

---

## 📌 Notes

* Make sure MySQL is running
* Create database before running project

---

## 📄 License

This project is developed for academic purposes.
