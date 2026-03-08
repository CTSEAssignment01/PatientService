package com.example.patientservice.service;

import com.example.patientservice.client.DoctorServiceClient;
import com.example.patientservice.dto.AppointmentRequest;
import com.example.patientservice.dto.AppointmentResponse;
import com.example.patientservice.dto.SlotDTO;
import com.example.patientservice.exception.AppointmentNotFoundException;
import com.example.patientservice.exception.ForbiddenException;
import com.example.patientservice.exception.SlotNotAvailableException;
import com.example.patientservice.model.Appointment;
import com.example.patientservice.model.AppointmentStatus;
import com.example.patientservice.model.Patient;
import com.example.patientservice.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientService patientService;
    private final DoctorServiceClient doctorServiceClient;

    @Override
    @Transactional
    public AppointmentResponse bookAppointment(AppointmentRequest request, UUID userId, String bearerToken) {
        // Get the patient profile for this user
        Patient patient = patientService.getPatientByUserId(userId);

        // Check if this slot is already booked (not cancelled)
        if (appointmentRepository.existsBySlotIdAndStatusNot(request.getSlotId(), AppointmentStatus.CANCELLED)) {
            throw new SlotNotAvailableException("This slot is already booked");
        }

        // Get slot details from doctor-service
        SlotDTO slot = doctorServiceClient.getSlot(request.getSlotId(), bearerToken);
        if (slot == null) {
            throw new SlotNotAvailableException("Slot not found");
        }

        // Create the appointment first to get its ID
        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctorId(request.getDoctorId())
                .doctorName(request.getDoctorName() != null ? request.getDoctorName() : slot.getDoctorName())
                .slotId(request.getSlotId())
                .date(slot.getDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .status(AppointmentStatus.BOOKED)
                .reason(request.getReason())
                .build();
        appointment = appointmentRepository.save(appointment);

        // Reserve the slot in doctor-service
        SlotDTO reserved = doctorServiceClient.reserveSlot(
                request.getSlotId(), patient.getId(), appointment.getId(), bearerToken);

        if (reserved == null) {
            // Compensation: delete the appointment if slot reservation failed
            appointmentRepository.delete(appointment);
            throw new SlotNotAvailableException("Slot is no longer available");
        }

        return toResponse(appointment);
    }

    @Override
    public AppointmentResponse getAppointmentById(UUID id) {
        return toResponse(findById(id));
    }

    @Override
    public List<AppointmentResponse> getAppointmentsByPatientId(UUID patientId) {
        return appointmentRepository.findByPatientIdOrderByDateDescStartTimeDesc(patientId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<AppointmentResponse> getAppointmentsByDoctorId(UUID doctorId) {
        return appointmentRepository.findByDoctorIdOrderByDateDescStartTimeDesc(doctorId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<AppointmentResponse> getMyAppointments(UUID userId) {
        Patient patient = patientService.getPatientByUserId(userId);
        return getAppointmentsByPatientId(patient.getId());
    }

    @Override
    @Transactional
    public AppointmentResponse cancelAppointment(UUID appointmentId, UUID userId, String bearerToken) {
        Appointment appointment = findById(appointmentId);

        // Only the patient who booked or an admin can cancel
        Patient patient = patientService.getPatientByUserId(userId);
        if (!appointment.getPatient().getId().equals(patient.getId())) {
            throw new ForbiddenException("You can only cancel your own appointments");
        }

        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new SlotNotAvailableException("Only booked appointments can be cancelled");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment = appointmentRepository.save(appointment);

        // Release the slot in doctor-service (compensation)
        try {
            doctorServiceClient.releaseSlot(appointment.getSlotId(), bearerToken);
        } catch (Exception e) {
            // Log but don't fail — the appointment is cancelled on our side
        }

        return toResponse(appointment);
    }

    @Override
    @Transactional
    public AppointmentResponse completeAppointment(UUID appointmentId) {
        Appointment appointment = findById(appointmentId);
        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new SlotNotAvailableException("Only booked appointments can be completed");
        }
        appointment.setStatus(AppointmentStatus.COMPLETED);
        return toResponse(appointmentRepository.save(appointment));
    }

    @Override
    @Transactional
    public AppointmentResponse addNotes(UUID appointmentId, String notes) {
        Appointment appointment = findById(appointmentId);
        appointment.setNotes(notes);
        return toResponse(appointmentRepository.save(appointment));
    }

    private Appointment findById(UUID id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException("Appointment not found with id: " + id));
    }

    private AppointmentResponse toResponse(Appointment a) {
        return AppointmentResponse.builder()
                .id(a.getId())
                .patientId(a.getPatient().getId())
                .patientName(a.getPatient().getName())
                .doctorId(a.getDoctorId())
                .doctorName(a.getDoctorName())
                .slotId(a.getSlotId())
                .date(a.getDate())
                .startTime(a.getStartTime())
                .endTime(a.getEndTime())
                .status(a.getStatus())
                .reason(a.getReason())
                .notes(a.getNotes())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
