package com.example.patientservice.controller;

import com.example.patientservice.dto.AppointmentRequest;
import com.example.patientservice.dto.AppointmentResponse;
import com.example.patientservice.dto.TokenValidationResponse;
import com.example.patientservice.service.AppointmentService;
import com.example.patientservice.util.AuthHelper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AuthHelper authHelper;

    public AppointmentController(AppointmentService appointmentService, AuthHelper authHelper) {
        this.appointmentService = appointmentService;
        this.authHelper = authHelper;
    }

    /**
     * Book a new appointment. Authenticated patients only.
     * Reserves the slot in doctor-service.
     */
    @PostMapping
    public ResponseEntity<AppointmentResponse> bookAppointment(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody AppointmentRequest request) {
        TokenValidationResponse auth = authHelper.requireAuthenticated(authHeader);
        String bearerToken = authHeader.substring(7);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.bookAppointment(request, auth.getUserId(), bearerToken));
    }

    /**
     * Get current user's appointments.
     */
    @GetMapping("/my")
    public ResponseEntity<List<AppointmentResponse>> getMyAppointments(
            @RequestHeader("Authorization") String authHeader) {
        TokenValidationResponse auth = authHelper.requireAuthenticated(authHeader);
        return ResponseEntity.ok(appointmentService.getMyAppointments(auth.getUserId()));
    }

    /**
     * Get an appointment by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> getAppointmentById(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id) {
        authHelper.requireAuthenticated(authHeader);
        return ResponseEntity.ok(appointmentService.getAppointmentById(id));
    }

    /**
     * Get all appointments for a specific patient. ADMIN/RECEPTIONIST/DOCTOR only.
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<AppointmentResponse>> getByPatient(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID patientId) {
        authHelper.requireRole(authHeader, "ADMIN", "RECEPTIONIST", "DOCTOR");
        return ResponseEntity.ok(appointmentService.getAppointmentsByPatientId(patientId));
    }

    /**
     * Get all appointments for a specific doctor. ADMIN/RECEPTIONIST/DOCTOR only.
     */
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<AppointmentResponse>> getByDoctor(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID doctorId) {
        authHelper.requireRole(authHeader, "ADMIN", "RECEPTIONIST", "DOCTOR");
        return ResponseEntity.ok(appointmentService.getAppointmentsByDoctorId(doctorId));
    }

    /**
     * Cancel an appointment. Releases the slot in doctor-service.
     * Only the patient who booked can cancel.
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponse> cancelAppointment(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id) {
        TokenValidationResponse auth = authHelper.requireAuthenticated(authHeader);
        String bearerToken = authHeader.substring(7);
        return ResponseEntity.ok(appointmentService.cancelAppointment(id, auth.getUserId(), bearerToken));
    }

    /**
     * Mark an appointment as completed. ADMIN/DOCTOR only.
     */
    @PatchMapping("/{id}/complete")
    public ResponseEntity<AppointmentResponse> completeAppointment(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id) {
        authHelper.requireRole(authHeader, "ADMIN", "DOCTOR");
        return ResponseEntity.ok(appointmentService.completeAppointment(id));
    }

    /**
     * Add/update notes for an appointment. ADMIN/DOCTOR only.
     */
    @PatchMapping("/{id}/notes")
    public ResponseEntity<AppointmentResponse> addNotes(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable UUID id,
            @RequestBody String notes) {
        authHelper.requireRole(authHeader, "ADMIN", "DOCTOR");
        return ResponseEntity.ok(appointmentService.addNotes(id, notes));
    }
}
