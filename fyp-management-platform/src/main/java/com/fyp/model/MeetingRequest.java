package com.fyp.model;

import com.fyp.enums.MeetingStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public class MeetingRequest {
    private UUID requestId;
    private LocalDateTime proposedTime;
    private String location;
    private String agenda;
    private MeetingStatus status;
    private LocalDateTime counterTime;
    private UUID studentId;
    private UUID supervisorId;

    public MeetingRequest() {}

    public MeetingRequest(UUID requestId, LocalDateTime proposedTime,
                          String location, String agenda, MeetingStatus status,
                          UUID studentId, UUID supervisorId) {
        this.requestId    = requestId;
        this.proposedTime = proposedTime;
        this.location     = location;
        this.agenda       = agenda;
        this.status       = status;
        this.studentId    = studentId;
        this.supervisorId = supervisorId;
    }

    public void accept()  { this.status = MeetingStatus.CONFIRMED; }
    public void decline() { this.status = MeetingStatus.DECLINED; }

    public void reschedule(LocalDateTime newTime) {
        this.counterTime = newTime;
        this.status      = MeetingStatus.RESCHEDULED;
    }

    public UUID getRequestId()          { return requestId; }
    public LocalDateTime getProposedTime(){ return proposedTime; }
    public String getLocation()         { return location; }
    public String getAgenda()           { return agenda; }
    public MeetingStatus getStatus()    { return status; }
    public LocalDateTime getCounterTime(){ return counterTime; }
    public UUID getStudentId()          { return studentId; }
    public UUID getSupervisorId()       { return supervisorId; }

    public void setRequestId(UUID requestId)               { this.requestId = requestId; }
    public void setProposedTime(LocalDateTime proposedTime){ this.proposedTime = proposedTime; }
    public void setLocation(String location)               { this.location = location; }
    public void setAgenda(String agenda)                   { this.agenda = agenda; }
    public void setStatus(MeetingStatus status)            { this.status = status; }
    public void setCounterTime(LocalDateTime counterTime)  { this.counterTime = counterTime; }
    public void setStudentId(UUID studentId)               { this.studentId = studentId; }
    public void setSupervisorId(UUID supervisorId)         { this.supervisorId = supervisorId; }
}
