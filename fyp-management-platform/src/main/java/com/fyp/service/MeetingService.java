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

    public UUID proposeMeeting(UUID supervisorId, LocalDateTime proposedTime,
                               String location, String agenda,
                               UUID supervisorUserId, String supervisorEmail) throws Exception {
        var user = SessionManager.getCurrentUser();
        if (!"STUDENT".equals(user.getRole()))
            throw new Exception("Only students can propose meetings.");

        Student student = (Student) user;
        MeetingRequest req = new MeetingRequest();
        req.setStudentId(student.getStudentId());
        req.setSupervisorId(supervisorId);
        req.setProposedTime(proposedTime);
        req.setLocation(location);
        req.setAgenda(agenda);
        req.setStatus(MeetingStatus.REQUESTED);

        UUID id = meetingDAO.insert(req);
        notifService.create(supervisorUserId, NotificationType.MEETING,
            "Meeting request from " + user.getName() + " on " + proposedTime,
            supervisorEmail);
        return id;
    }

    public void acceptMeeting(UUID requestId, UUID studentUserId, String studentEmail) throws Exception {
        meetingDAO.updateStatus(requestId, MeetingStatus.CONFIRMED);
        notifService.create(studentUserId, NotificationType.MEETING,
            "Your meeting request has been CONFIRMED.", studentEmail);
    }

    public void declineMeeting(UUID requestId, UUID studentUserId, String studentEmail) throws Exception {
        meetingDAO.updateStatus(requestId, MeetingStatus.DECLINED);
        notifService.create(studentUserId, NotificationType.MEETING,
            "Your meeting request has been DECLINED.", studentEmail);
    }

    public void rescheduleMeeting(UUID requestId, LocalDateTime newTime,
                                  UUID studentUserId, String studentEmail) throws Exception {
        meetingDAO.reschedule(requestId, newTime);
        notifService.create(studentUserId, NotificationType.MEETING,
            "Your meeting has been rescheduled to " + newTime, studentEmail);
    }

    public List<MeetingRequest> getMeetingsForCurrentUser() throws Exception {
        var user = SessionManager.getCurrentUser();
        return switch (user.getRole()) {
            case "STUDENT"    -> meetingDAO.findByStudentId(((Student) user).getStudentId());
            case "SUPERVISOR" -> meetingDAO.findBySupervisorId(((Supervisor) user).getSupervisorId());
            default           -> List.of();
        };
    }
}
