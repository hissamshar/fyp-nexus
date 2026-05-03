package com.fyp.service;

import com.fyp.dao.MeetingRequestDAO;
import com.fyp.enums.MeetingStatus;
import com.fyp.enums.NotificationType;
import com.fyp.model.*;
import com.fyp.util.SessionManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class MeetingService {

    private final MeetingRequestDAO meetingDAO = new MeetingRequestDAO();
    private final NotificationService notifService = new NotificationService();

    private String jwt() { return SessionManager.getJwtToken(); }

    public UUID proposeMeeting(UUID supervisorId, LocalDateTime proposedTime,
                               String location, String agenda,
                               UUID supervisorUserId, String supervisorEmail) throws Exception {
        var user = SessionManager.getCurrentUser();
        if (!"STUDENT".equals(user.getRole()))
            throw new Exception("Only students can propose meetings.");

        Student student = (user instanceof Student s) ? s
            : new com.fyp.dao.StudentDAO().findByUserId(user.getUserId(), jwt())
                .orElseThrow(() -> new Exception("Student profile not found."));
        MeetingRequest req = new MeetingRequest();
        req.setStudentId(student.getStudentId());
        req.setSupervisorId(supervisorId);
        req.setProposedTime(proposedTime);
        req.setLocation(location);
        req.setAgenda(agenda);
        req.setStatus(MeetingStatus.REQUESTED);

        UUID id = meetingDAO.insert(req, jwt());
        notifService.create(supervisorUserId, NotificationType.MEETING,
            "Meeting request from " + user.getName() + " on " + proposedTime,
            supervisorEmail);
        return id;
    }

    public void acceptMeeting(UUID requestId, UUID studentUserId, String studentEmail) throws Exception {
        meetingDAO.updateStatus(requestId, MeetingStatus.CONFIRMED, jwt());
        notifService.create(studentUserId, NotificationType.MEETING,
            "Your meeting request has been CONFIRMED.", studentEmail);
    }

    public void declineMeeting(UUID requestId, UUID studentUserId, String studentEmail) throws Exception {
        meetingDAO.updateStatus(requestId, MeetingStatus.DECLINED, jwt());
        notifService.create(studentUserId, NotificationType.MEETING,
            "Your meeting request has been DECLINED.", studentEmail);
    }

    public void rescheduleMeeting(UUID requestId, LocalDateTime newTime,
                                  UUID studentUserId, String studentEmail) throws Exception {
        meetingDAO.reschedule(requestId, newTime, jwt());
        notifService.create(studentUserId, NotificationType.MEETING,
            "Your meeting has been rescheduled to " + newTime, studentEmail);
    }

    public List<MeetingRequest> getMeetingsForCurrentUser() throws Exception {
        var user = SessionManager.getCurrentUser();
        return switch (user.getRole()) {
            case "STUDENT" -> {
                Student s = (user instanceof Student st) ? st
                    : new com.fyp.dao.StudentDAO().findByUserId(user.getUserId(), jwt()).orElseThrow();
                yield meetingDAO.findByStudentId(s.getStudentId(), jwt());
            }
            case "SUPERVISOR" -> {
                Supervisor sv = (user instanceof Supervisor sup) ? sup
                    : new com.fyp.dao.SupervisorDAO().findByUserId(user.getUserId(), jwt()).orElseThrow();
                yield meetingDAO.findBySupervisorId(sv.getSupervisorId(), jwt());
            }
            default -> List.of();
        };
    }

    // ── Adapter methods for MeetingSchedulerController ────────────────────────

    public List<Supervisor> getAvailableSupervisors(String token) {
        try { return new com.fyp.dao.SupervisorDAO().findAll(token); }
        catch (Exception e) { e.printStackTrace(); return java.util.List.of(); }
    }

    public MeetingRequest requestMeeting(UUID supervisorId, String dateTimeStr,
                                         String agenda, String token) {
        try {
            var user = SessionManager.getCurrentUser();
            MeetingRequest req = new MeetingRequest();
            req.setSupervisorId(supervisorId);
            if ("STUDENT".equals(user.getRole())) {
                Student s = (user instanceof Student st) ? st
                    : new com.fyp.dao.StudentDAO().findByUserId(user.getUserId(), token).orElse(null);
                if (s != null) req.setStudentId(s.getStudentId());
            }
            req.setProposedTime(LocalDateTime.parse(dateTimeStr.replace(" ", "T")));
            req.setAgenda(agenda);
            req.setStatus(MeetingStatus.REQUESTED);
            UUID id = meetingDAO.insert(req, token);
            req.setMeetingId(id);
            return req;
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    public boolean updateStatus(UUID meetingId, String status, String token) {
        try {
            MeetingStatus ms = MeetingStatus.valueOf(status);
            meetingDAO.updateStatus(meetingId, ms, token);
            return true;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }
}
