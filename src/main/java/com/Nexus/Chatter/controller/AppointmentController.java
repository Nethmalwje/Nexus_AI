package com.Nexus.Chatter.controller;

import com.Nexus.Chatter.model.Appointment;
import com.Nexus.Chatter.service.AppointmentService;
import com.Nexus.Chatter.service.GoogleCalendarService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final GoogleCalendarService googleCalendarService;

    public AppointmentController(AppointmentService appointmentService,
                                 GoogleCalendarService googleCalendarService) {
        this.appointmentService = appointmentService;
        this.googleCalendarService = googleCalendarService;
    }

    // ===============================
    // CREATE (PENDING)
    // ===============================
    @PostMapping("/{botId}")
    public ResponseEntity<?> create(@PathVariable UUID botId,
                                    @RequestBody Map<String, String> body) {

        String visitorSessionId = body.get("visitorSessionId");
        String serviceName = body.get("serviceName");
        String startTimeStr = body.get("startTime");
        String endTimeStr = body.get("endTime");

        if (visitorSessionId == null || serviceName == null
                || startTimeStr == null || endTimeStr == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "MISSING_FIELDS"
            ));
        }

        Appointment saved = appointmentService.createAppointment(
                botId,
                visitorSessionId,
                serviceName,
                LocalDateTime.parse(startTimeStr),
                LocalDateTime.parse(endTimeStr),
                body.get("customerName"),
                body.get("customerContact")
        );

        return ResponseEntity.ok(saved);
    }

    // ===============================
    // LIST
    // ===============================
    @GetMapping("/{botId}")
    public List<Appointment> listBot(@PathVariable UUID botId) {
        return appointmentService.listBotAppointments(botId);
    }

    @GetMapping("/{botId}/visitor/{visitorSessionId}")
    public List<Appointment> listVisitor(@PathVariable UUID botId,
                                         @PathVariable String visitorSessionId) {
        return appointmentService.listVisitorAppointments(botId, visitorSessionId);
    }

    // ===============================
    // AVAILABILITY (Phase 3.2)
    // ===============================
    @PostMapping("/{botId}/availability")
    public ResponseEntity<?> availability(@PathVariable UUID botId,
                                          @RequestBody Map<String, String> body) {

        boolean available = googleCalendarService.isSlotAvailable(
                botId,
                LocalDateTime.parse(body.get("startTime")),
                LocalDateTime.parse(body.get("endTime"))
        );

        return ResponseEntity.ok(Map.of(
                "botId", botId.toString(),
                "available", available
        ));
    }

    // ===============================
    // CONFIRM (Phase 3.3)
    // ===============================
    @PostMapping("/confirm/{appointmentId}")
    public ResponseEntity<?> confirm(@PathVariable UUID appointmentId) {

        AppointmentService.ConfirmResult result =
                appointmentService.confirmAppointment(appointmentId);

        if (result.ok) {
            return ResponseEntity.ok(result.appointment);
        }

        return ResponseEntity.status(result.httpStatus).body(Map.of(
                "error", "CONFIRM_FAILED",
                "message", result.message
        ));
    }
}
