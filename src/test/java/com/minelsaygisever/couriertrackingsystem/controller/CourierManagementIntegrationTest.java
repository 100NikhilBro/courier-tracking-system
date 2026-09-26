package com.minelsaygisever.couriertrackingsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minelsaygisever.couriertrackingsystem.domain.CourierProfile;
import com.minelsaygisever.couriertrackingsystem.domain.User;
import com.minelsaygisever.couriertrackingsystem.dto.courier.CreateCourierRequest;
import com.minelsaygisever.couriertrackingsystem.dto.courier.UpdateCourierRequest;
import com.minelsaygisever.couriertrackingsystem.repository.CourierProfileRepository;
import com.minelsaygisever.couriertrackingsystem.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static com.minelsaygisever.couriertrackingsystem.domain.Role.ROLE_COURIER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class CourierManagementIntegrationTest {

    @TestConfiguration
    static class NoRedisCacheConfig {

        @Bean
        public CacheManager cacheManager() {
            return new NoOpCacheManager();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourierProfileRepository courierProfileRepository;

    @Test
    @DisplayName("Should create courier successfully when user is ADMIN")
    @WithMockUser(roles = {"ADMIN"})
    void createCourier_Success() throws Exception {
        // Given
        CreateCourierRequest request = new CreateCourierRequest();
        request.setUsername("motoCourier1");
        request.setPassword("securePass123");
        request.setFullName("Speedy Gonzales");

        // When & Then
        mockMvc.perform(post("/api/v1/couriers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("motoCourier1")))
                .andExpect(jsonPath("$.fullName", is("Speedy Gonzales")))
                .andExpect(jsonPath("$.totalDistanceInMeters", is(0.0)));
    }

    @Test
    @DisplayName("Should return 403 Forbidden when a COURIER tries to create another courier")
    @WithMockUser(roles = {"COURIER"})
    void createCourier_Forbidden_For_Courier() throws Exception {
        CreateCourierRequest request = new CreateCourierRequest();
        request.setUsername("newCourier");
        request.setPassword("pass123456");
        request.setFullName("Unprivileged User");

        mockMvc.perform(post("/api/v1/couriers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 409 Conflict when username already exists")
    @WithMockUser(roles = {"ADMIN"})
    void createCourier_DuplicateUsername() throws Exception {
        CreateCourierRequest request = new CreateCourierRequest();
        request.setUsername("duplicateCourier");
        request.setPassword("pass123456");
        request.setFullName("First One");

        mockMvc.perform(post("/api/v1/couriers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/couriers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Username already exists: duplicateCourier")));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when password is too short")
    @WithMockUser(roles = {"ADMIN"})
    void createCourier_ValidationFail() throws Exception {
        CreateCourierRequest request = new CreateCourierRequest();
        request.setUsername("validUser");
        request.setPassword("123");
        request.setFullName("Valid Name");

        mockMvc.perform(post("/api/v1/couriers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    @DisplayName("Should delete User when CourierProfile is deleted (Cascade Check)")
    @WithMockUser(roles = {"ADMIN"})
    void deleteCourier_ShouldCascadeToUser() throws Exception {
        CreateCourierRequest request = new CreateCourierRequest();
        request.setUsername("courierToBeDeleted");
        request.setPassword("pass123456");
        request.setFullName("Delete Me Courier");

        // 1. Create via API
        mockMvc.perform(post("/api/v1/couriers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // 2. Get IDs
        User user = userRepository.findByUsername("courierToBeDeleted").orElseThrow();
        CourierProfile profile = courierProfileRepository.findByUserId(user.getId()).orElseThrow();

        Long userId = user.getId();
        Long profileId = profile.getId();

        // 3. Delete via API
        mockMvc.perform(delete("/api/v1/couriers/" + userId))
                .andExpect(status().isOk());

        // 4. Verify Soft Delete logic
        User deletedUser = userRepository.findById(userId).orElseThrow();
        CourierProfile savedProfile = courierProfileRepository.findById(profile.getId()).orElseThrow();

        assertThat(deletedUser.isEnabled()).isFalse();
        assertThat(savedProfile).isNotNull();
    }

    @Test
    @DisplayName("Should update both CourierProfile and User entity successfully")
    @WithMockUser(roles = {"ADMIN"})
    void updateCourier_Success() throws Exception {
        // 1. Setup Data
        User user = User.builder()
                .username("courierUpdateTarget")
                .password("oldPass")
                .role(ROLE_COURIER)
                .isEnabled(true)
                .build();

        CourierProfile profile = CourierProfile.builder()
                .user(user)
                .fullName("Old Courier Name")
                .totalDistanceInMeters(50.0)
                .build();

        courierProfileRepository.save(profile);

        Long userId = user.getId();
        Long profileId = profile.getId();

        // 2. Create Update Request
        UpdateCourierRequest updateRequest = new UpdateCourierRequest();
        updateRequest.setFullName("Updated Courier Name");
        updateRequest.setPassword("newSecurePass123");
        updateRequest.setEnabled(false);

        // 3. Perform Update
        mockMvc.perform(put("/api/v1/couriers/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("Updated Courier Name")))
                .andExpect(jsonPath("$.enabled", is(false)));

        // 4. Verify DB State
        CourierProfile updatedProfile = courierProfileRepository.findById(profileId).orElseThrow();

        assertThat(updatedProfile.getFullName()).isEqualTo("Updated Courier Name");
        assertThat(updatedProfile.getUser().isEnabled()).isFalse();
        assertThat(updatedProfile.getUser().getPassword()).isNotEqualTo("oldPass");
        assertThat(updatedProfile.getTotalDistanceInMeters()).isEqualTo(50.0);
    }
}