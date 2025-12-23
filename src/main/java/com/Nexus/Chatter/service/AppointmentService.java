package com.Nexus.Chatter.service;

import com.Nexus.Chatter.model.Appointment;
import com.Nexus.Chatter.model.Chatbot;
import com.Nexus.Chatter.repo.AppointmentRepo;
import com.Nexus.Chatter.repo.ChatbotRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AppointmentService {

    private final AppointmentRepo appointmentRepository;
    private final ChatbotRepo chatbotRepo;
    private final GoogleCalendarService googleCalendarService;

    public AppointmentService(AppointmentRepo appointmentRepository,
                              ChatbotRepo chatbotRepo,
                              GoogleCalendarService googleCalendarService) {
        this.appointmentRepository = appointmentRepository;
        this.chatbotRepo = chatbotRepo;
        this.googleCalendarService = googleCalendarService;
    }

    // ===============================
    // CREATE (PENDING)
    // ===============================
    @Transactional
    public Appointment createAppointment(UUID botId,
                                         String visitorSessionId,
                                         String serviceName,
                                         LocalDateTime start,
                                         LocalDateTime end,
                                         String name,
                                         String contact) {

        Chatbot bot = chatbotRepo.findById(botId)
                .orElseThrow(() -> new RuntimeException("Bot not found"));

        Appointment a = new Appointment();
        a.setChatbot(bot);
        a.setVisitorSessionId(visitorSessionId);
        a.setServiceName(serviceName);
        a.setStartTime(start);
        a.setEndTime(end);
        a.setCustomerName(name);
        a.setCustomerContact(contact);
        a.setStatus("PENDING");

        return appointmentRepository.save(a);
    }

    // ===============================
    // LIST / READ
    // ===============================
    public List<Appointment> listBotAppointments(UUID botId) {
        return appointmentRepository.findByChatbotIdOrderByStartTimeDesc(botId);
    }

    public List<Appointment> listVisitorAppointments(UUID botId, String visitorSessionId) {
        return appointmentRepository
                .findByChatbotIdAndVisitorSessionIdOrderByStartTimeDesc(botId, visitorSessionId);
    }

    // ===============================
    // CONFIRM (Phase 3.3)
    // ===============================
    public static class ConfirmResult {
        public boolean ok;
        public int httpStatus;
        public String message;
        public Appointment appointment;

        static ConfirmResult ok(Appointment a) {
            var r = new ConfirmResult();
            r.ok = true;
            r.httpStatus = 200;
            r.appointment = a;
            r.message = "CONFIRMED";
            return r;
        }

        static ConfirmResult conflict(String m) {
            var r = new ConfirmResult();
            r.ok = false;
            r.httpStatus = 409;
            r.message = m;
            return r;
        }

        static ConfirmResult error(String m) {
            var r = new ConfirmResult();
            r.ok = false;
            r.httpStatus = 500;
            r.message = m;
            return r;
        }
    }

    /**
     * Wrapper (NOT transactional) - catches AFTER tx ends.
     * Prevents UnexpectedRollbackException.
     */
    public ConfirmResult confirmAppointment(UUID appointmentId) {
        try {
            return confirmAppointmentTx(appointmentId);
        } catch (Exception e) {
            return ConfirmResult.error(e.getMessage());
        }
    }

    /**
     * Actual transactional confirm logic (NO try/catch here).
     */
    @Transactional
    protected ConfirmResult confirmAppointmentTx(UUID appointmentId) {

        Appointment a = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        UUID botId = a.getChatbot().getId();

        // 🔒 cluster-safe lock
        // IMPORTANT: your repo method can be void OR Long. We ignore the return.
        appointmentRepository.lockForBot("bot:" + botId);

        // Idempotent confirm
        if ("CONFIRMED".equalsIgnoreCase(a.getStatus()) && a.getGoogleEventId() != null) {
            return ConfirmResult.ok(a);
        }

        // DB overlap guard
        if (appointmentRepository.hasConfirmedOverlap(botId, a.getStartTime(), a.getEndTime())) {
            return ConfirmResult.conflict("Time slot already booked.");
        }

        // Google freeBusy guard
        if (!googleCalendarService.isSlotAvailable(botId, a.getStartTime(), a.getEndTime())) {
            return ConfirmResult.conflict("Calendar busy.");
        }

        // Create event
        String eventId = googleCalendarService.createEvent(
                botId,
                "Booking: " + a.getServiceName(),
                "Visitor: " + a.getVisitorSessionId(),
                a.getStartTime(),
                a.getEndTime()
        );

        a.setGoogleEventId(eventId);
        a.setStatus("CONFIRMED");

        return ConfirmResult.ok(appointmentRepository.save(a));
    }
}
