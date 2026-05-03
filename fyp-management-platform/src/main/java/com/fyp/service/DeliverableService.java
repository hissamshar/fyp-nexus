package com.fyp.service;

import com.fyp.dao.DeliverableDAO;
import com.fyp.model.Deliverable;
import com.fyp.model.Student;
import com.fyp.util.AuditLogger;
import com.fyp.util.FileValidator;
import com.fyp.util.SessionManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class DeliverableService {

    private final DeliverableDAO deliverableDAO = new DeliverableDAO();

    private String jwt() { return SessionManager.getJwtToken(); }

    private static final String UPLOAD_DIR;

    static {
        String dir = "uploads";
        try (var is = DeliverableService.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (is != null) {
                Properties p = new Properties();
                p.load(is);
                dir = p.getProperty("app.upload.dir", "uploads");
            }
        } catch (Exception ignored) {}
        File cfg = new File("config.properties");
        if (cfg.exists()) {
            try (var fis = new java.io.FileInputStream(cfg)) {
                Properties p = new Properties();
                p.load(fis);
                dir = p.getProperty("app.upload.dir", dir);
            } catch (Exception ignored) {}
        }
        UPLOAD_DIR = dir;
    }

    public UUID uploadDeliverable(UUID milestoneId, File file) throws Exception {
        var user = SessionManager.getCurrentUser();
        if (!"STUDENT".equals(user.getRole()))
            throw new Exception("Only students can upload deliverables.");

        if (!FileValidator.isUnderSizeLimit(file))
            throw new Exception("FILE_TOO_LARGE");

        if (!FileValidator.isAllowedType(file))
            throw new Exception("INVALID_FILE_TYPE");

        Path uploadPath = Paths.get(UPLOAD_DIR, milestoneId.toString());
        Files.createDirectories(uploadPath);

        String safeName = file.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
        Path dest = uploadPath.resolve(safeName);

        Files.copy(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);

        String sanitisedPath = FileValidator.sanitisePath(dest.toAbsolutePath().toString());

        Student student = (user instanceof Student s) ? s
            : new com.fyp.dao.StudentDAO().findByUserId(user.getUserId(), jwt())
                .orElseThrow(() -> new Exception("Student profile not found."));
        Deliverable d = new Deliverable();
        d.setFileName(safeName);
        d.setFileType(FileValidator.getExtension(safeName));
        d.setFilePath(sanitisedPath);
        d.setMilestoneId(milestoneId);
        d.setStudentId(student.getStudentId());
        d.setUploadTimestamp(LocalDateTime.now());

        UUID fileId = deliverableDAO.insert(d, jwt());
        AuditLogger.logFileUpload(user.getUserId(), safeName);
        return fileId;
    }

    public List<Deliverable> getForMilestone(UUID milestoneId) throws Exception {
        return deliverableDAO.findByMilestoneId(milestoneId, jwt());
    }

    public List<Deliverable> getForStudent(UUID studentId) throws Exception {
        return deliverableDAO.findByStudentId(studentId, jwt());
    }

    public void deleteDeliverable(UUID fileId, String filePath) throws Exception {
        deliverableDAO.delete(fileId, jwt());
        try { Files.deleteIfExists(Paths.get(filePath)); } catch (IOException ignored) {}
    }

    // ── Adapter Methods ────────────────────────────────────────────────────────
    public List<Deliverable> getDeliverablesForCurrentUser(String token) {
        try {
            return getForStudent(SessionManager.getCurrentUser().getUserId());
        } catch (Exception e) { e.printStackTrace(); return List.of(); }
    }

    public Deliverable uploadDeliverable(String title, String desc, UUID milestoneId, File file, String token) {
        try {
            UUID id = uploadDeliverable(milestoneId, file);
            Deliverable d = new Deliverable();
            d.setFileId(id);
            d.setTitle(title);
            d.setDescription(desc);
            d.setMilestoneId(milestoneId);
            return d;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }
}
