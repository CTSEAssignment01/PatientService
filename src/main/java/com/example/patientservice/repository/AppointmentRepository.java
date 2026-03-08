package com.example.patientservice.repository;

import com.example.patientservice.model.Appointment;
import com.example.patientservice.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findByPatientIdOrderByDateDescStartTimeDesc(UUID patientId);

    List<Appointment> findByDoctorIdOrderByDateDescStartTimeDesc(UUID doctorId);

    List<Appointment> findByPatientIdAndStatusOrderByDateAscStartTimeAsc(UUID patientId, AppointmentStatus status);

    List<Appointment> findByDoctorIdAndStatusOrderByDateAscStartTimeAsc(UUID doctorId, AppointmentStatus status);

    boolean existsBySlotIdAndStatusNot(UUID slotId, AppointmentStatus status);
}
