# FYP Management Platform

A desktop application for managing Final Year Projects at FAST NUCES Peshawar.

## Tech Stack
- Java 21 + JavaFX 21 (FXML)
- Maven build system
- Supabase (PostgreSQL) via JDBC + HikariCP
- BCrypt password hashing
- JavaMail for OTP/notifications
- iText 8 for PDF report generation

## Setup

1. **Configure database**:
   - Copy `config.properties.template` to `config.properties`
   - Fill in your Supabase DB password and SMTP credentials
   - Run `src/main/resources/db/schema.sql` on your Supabase project

2. **Build & Run**:
   ```bash
   mvn clean compile
   mvn javafx:run
   ```

## Architecture
Strict MVC: View (FXML) → Controller → Service → DAO → Database

## Default Roles
- **Admin**: Full system access
- **Supervisor**: Review proposals, grade students
- **Student**: Submit proposals, upload deliverables
- **Examiner**: Evaluate projects, submit grades
- **Industry Partner**: Post problems, view adopted projects
