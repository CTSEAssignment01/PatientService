package com.example.patientservice.client;

import com.example.patientservice.dto.SlotDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DoctorServiceClient {

    private final RestTemplate restTemplate;

    @Value("${doctor-service.url}")
    private String doctorServiceUrl;

    /**
     * Fetches slot details from the doctor-service.
     */
    public SlotDTO getSlot(UUID slotId, String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<SlotDTO> response = restTemplate.exchange(
                doctorServiceUrl + "/api/slots/" + slotId,
                HttpMethod.GET,
                entity,
                SlotDTO.class);
        return response.getBody();
    }

    /**
     * Reserves a slot in the doctor-service for a patient appointment.
     */
    public SlotDTO reserveSlot(UUID slotId, UUID patientId, UUID appointmentId, String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "patientId", patientId.toString(),
                "appointmentId", appointmentId.toString()
        );
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<SlotDTO> response = restTemplate.exchange(
                    doctorServiceUrl + "/api/slots/" + slotId + "/reserve",
                    HttpMethod.POST,
                    entity,
                    SlotDTO.class);
            return response.getBody();
        } catch (HttpClientErrorException.Conflict e) {
            return null; // Slot not available
        }
    }

    /**
     * Releases a previously reserved slot (compensation for cancelled appointment).
     */
    public SlotDTO releaseSlot(UUID slotId, String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(bearerToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<SlotDTO> response = restTemplate.exchange(
                doctorServiceUrl + "/api/slots/" + slotId + "/release",
                HttpMethod.POST,
                entity,
                SlotDTO.class);
        return response.getBody();
    }
}
