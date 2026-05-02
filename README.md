# FYP Management Platform

A desktop application for managing Final Year Projects at FAST NUCES Peshawar. 

The platform simplifies project proposals, evaluations, deliverables tracking, and grading for all stakeholders, including Administrators, Supervisors, Students, Examiners, and Industry Partners.

## 🚀 Tech Stack

- **Frontend**: JavaFX 21 (FXML)
- **Backend**: Java 21
- **Database**: Supabase (PostgreSQL) via JDBC + HikariCP
- **Build System**: Maven
- **Security**: BCrypt password hashing
- **Communications**: JavaMail for OTP & email notifications
- **Reporting**: iText 8 for PDF report generation

## 📋 Prerequisites

Before running this project, you will need:
- **Java Development Kit (JDK) 21** or higher.
- (Optional) **Maven** - A built-in startup script `run.sh` will automatically download and use Maven if it's not installed on your system.
- A **Supabase** account (or local PostgreSQL instance) for the database.
- An **SMTP account** (e.g., Gmail with App Passwords enabled) to send OTPs and notifications.

## 🛠️ Setup & Installation

### 1. Clone the repository
```bash
git clone https://github.com/hissamshar/fyp-nexus
cd fyp-management-platform
```

### 2. Configure Database and Services
1. Locate the configuration template: `config.properties.template`
2. Copy it to create your active configuration file:
   ```bash
   cp config.properties.template config.properties
   ```
3. Open `config.properties` and fill in:
   - Your **Supabase DB details** (URL, username, and password)
   - Your **SMTP credentials** (email address and App Password)

### 3. Initialize the Database
1. Go to your Supabase project's SQL Editor.
2. Open the file `src/main/resources/db/schema.sql` from this repository.
3. Copy its contents, paste them into the SQL Editor, and run the queries to create the necessary tables and schemas.

## 🏃‍♂️ Running the Application

You can easily launch the application using the included startup script.

### Using the Startup Script (Linux/macOS)
Make sure the script has execute permissions, then run it:
```bash
chmod +x run.sh
./run.sh
```
*(Note: If Maven is not installed on your system, the script will automatically download a local copy of Maven into your `~/.local` folder and use it to build and run the app.)*

### Using Maven Directly
If you already have Maven configured on your system, you can build and run using:
```bash
mvn clean compile
mvn javafx:run
```

## 📥 Downloading the Pre-compiled Binary (Releases)

If you don't want to build the project from source, you can simply download the ready-to-use application from the GitHub Releases page.

1. Navigate to the [Releases](https://github.com/hissamshar/fyp-nexus/releases) page of this repository.
2. Download the `.zip` file corresponding to your Operating System (e.g., `FYPPlatform-Linux-x64.zip`).
3. Extract the downloaded zip file to your preferred location.
4. Open the extracted folder and run the executable file inside the `bin/` directory (e.g., `./bin/FYPPlatform`).

*No Java or Maven installation is required to run the pre-compiled binary!*

## 📦 Creating a Native Standalone Binary

You can generate a standalone, self-contained native executable (e.g., a `.exe` for Windows, `.app` for macOS, or binary folder for Linux) that doesn't require users to install Java or Maven.

To build the native binary, simply run the included build script:
```bash
chmod +x build-binary.sh
./build-binary.sh
```

Once the script finishes, your self-contained app will be located in the `dist/FYPPlatform/` directory. You can run it directly:
```bash
./dist/FYPPlatform/bin/FYPPlatform
```
*Note: The binary will be built for the Operating System you run the script on.*

## 🏗️ Architecture
The project strictly follows the **MVC (Model-View-Controller)** pattern:
`View (FXML) → Controller → Service → DAO → Database`

## 👥 User Roles
- **Admin**: Full system access, oversees users and platform configuration.
- **Supervisor**: Reviews student proposals, tracks project deliverables, and submits grades.
- **Student**: Submits project proposals, uploads deliverables, and tracks project progress.
- **Examiner**: Evaluates specific projects and submits examiner grades.
- **Industry Partner**: Posts industry-related problems and views adopted projects.
