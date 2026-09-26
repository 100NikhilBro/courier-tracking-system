package com.minelsaygisever.couriertrackingsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minelsaygisever.couriertrackingsystem.domain.AdminProfile;
import com.minelsaygisever.couriertrackingsystem.domain.User;
import com.minelsaygisever.couriertrackingsystem.dto.admin.CreateAdminRequest;
import com.minelsaygisever.couriertrackingsystem.dto.admin.UpdateAdminRequest;
import com.minelsaygisever.couriertrackingsystem.repository.AdminProfileRepository;
import com.minelsaygisever.couriertrackingsystem.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import static com.minelsaygisever.couriertrackingsystem.domain.Role.ROLE_ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AdminManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminProfileRepository adminProfileRepository;

    @Test
    @DisplayName("Should create admin successfully when user is ADMIN")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void createAdmin_Success() throws Exception {
        // Given
        CreateAdminRequest request = new CreateAdminRequest();
        request.setUsername("newAdmin");
        request.setPassword("pass123456");
        request.setFullName("New Test Admin");

        // When & Then
        mockMvc.perform(post("/api/v1/admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("newAdmin")))
                .andExpect(jsonPath("$.fullName", is("New Test Admin")));
    }

    @Test
    @DisplayName("Should return 403 Forbidden when COURIER tries to create admin")
    @WithMockUser(username = "courier", roles = {"COURIER"})
    void createAdmin_Forbidden_For_Courier() throws Exception {
        // Given
        CreateAdminRequest request = new CreateAdminRequest();
        request.setUsername("hackerAdmin");
        request.setPassword("pass123456");
        request.setFullName("Hacker");

        // When & Then
        mockMvc.perform(post("/api/v1/admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 409 Conflict when username already exists")
    @WithMockUser(roles = {"ADMIN"})
    void createAdmin_DuplicateUsername() throws Exception {
        CreateAdminRequest request = new CreateAdminRequest();
        request.setUsername("duplicateUser");
        request.setPassword("pass123456");
        request.setFullName("Original User");

        mockMvc.perform(post("/api/v1/admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Username already exists: duplicateUser")));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when password is too short")
    @WithMockUser(roles = {"ADMIN"})
    void createAdmin_ValidationFail() throws Exception {
        CreateAdminRequest request = new CreateAdminRequest();
        request.setUsername("shortPassUser");
        request.setPassword("123");
        request.setFullName("Invalid User");

        // When & Then
        mockMvc.perform(post("/api/v1/admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    @DisplayName("Should delete User when AdminProfile is deleted (Cascade Check)")
    @WithMockUser(roles = {"ADMIN"})
    void deleteAdmin_ShouldCascadeToUser() throws Exception {
        CreateAdminRequest request = new CreateAdminRequest();
        request.setUsername("toBeDeleted");
        request.setPassword("pass123456");
        request.setFullName("Delete Me");

        mockMvc.perform(post("/api/v1/admins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Long userId = userRepository.findByUsername("toBeDeleted").orElseThrow().getId();
        Long profileId = adminProfileRepository.findByUserId(userId).orElseThrow().getId();

        mockMvc.perform(delete("/api/v1/admins/" + userId))
                .andExpect(status().isOk());

        User deactivatedUser = userRepository.findById(userId).orElseThrow();
        AdminProfile savedProfile = adminProfileRepository.findById(profileId).orElseThrow();

        assertThat(deactivatedUser.isEnabled()).isFalse();
        assertThat(savedProfile).isNotNull();
    }

    @Test
    @DisplayName("Should update both Profile and User entity successfully")
    @WithMockUser(roles = {"ADMIN"})
    void updateAdmin_Success() throws Exception {
        User user = User.builder()
                .username("updateTarget")
                .password("oldPass")
                .role(ROLE_ADMIN)
                .isEnabled(true)
                .build();

        AdminProfile profile = AdminProfile.builder()
                .user(user)
                .fullName("Old Name")
                .build();

        adminProfileRepository.save(profile);

        Long userId = user.getId();
        Long profileId = profile.getId();

        UpdateAdminRequest updateRequest = new UpdateAdminRequest();
        updateRequest.setFullName("Updated Name");
        updateRequest.setPassword("newSecurePass123");
        updateRequest.setEnabled(false);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/admins/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("Updated Name")))
                .andExpect(jsonPath("$.enabled", is(false)));

        AdminProfile updatedProfile = adminProfileRepository.findById(profileId).orElseThrow();

        assertThat(updatedProfile.getFullName()).isEqualTo("Updated Name");
        assertThat(updatedProfile.getUser().isEnabled()).isFalse();
        assertThat(updatedProfile.getUser().getPassword()).isNotEqualTo("oldPass");
    }
}