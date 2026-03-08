package com.example.patientservice.service;

import com.example.patientservice.dto.AppointmentRequest;
import com.example.patientservice.dto.AppointmentResponse;

import java.util.List;
import java.util.UUID;

public interface AppointmentService {

    AppointmentResponse bookAppointment(AppointmentRequest request, UUID userId, String bearerToken);

    AppointmentResponse getAppointmentById(UUID id);

    List<AppointmentResponse> getAppointmentsByPatientId(UUID patientId);

    List<AppointmentResponse> getAppointmentsByDoctorId(UUID doctorId);

    List<AppointmentResponse> getMyAppointments(UUID userId);

    AppointmentResponse cancelAppointment(UUID appointmentId, UUID userId, String bearerToken);

    AppointmentResponse completeAppointment(UUID appointmentId);

    AppointmentResponse addNotes(UUID appointmentId, String notes);
}
