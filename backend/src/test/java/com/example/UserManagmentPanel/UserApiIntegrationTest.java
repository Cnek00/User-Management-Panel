package com.example.UserManagmentPanel;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class UserApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanListSeededUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users").with(httpBasic("admin", "Admin123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].username").value("admin"))
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    @Test
    void moderatorCanReadButCannotCreate() throws Exception {
        mockMvc.perform(get("/api/v1/users").with(httpBasic("moderator", "Moderator123!")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/users")
                        .with(httpBasic("moderator", "Moderator123!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"blocked","email":"blocked@example.com","password":"Password123!"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void regularUserCannotReadUserAdministration() throws Exception {
        mockMvc.perform(get("/api/v1/users").with(httpBasic("user", "User123!")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateUserAndPasswordIsNotSerialized() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .with(httpBasic("admin", "Admin123!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "newuser",
                                  "email": "newuser@example.com",
                                  "password": "Password123!",
                                  "roles": ["ROLE_USER"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.rank").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void duplicateUsernameReturnsConflict() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .with(httpBasic("admin", "Admin123!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "email": "another@example.com",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidPayloadReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .with(httpBasic("admin", "Admin123!"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"ab","email":"not-an-email","password":"short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void moderatorCanViewPermissionPageButCannotGrant() throws Exception {
        mockMvc.perform(get("/users/2/permissions")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("moderator").roles("MODERATOR")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("cannot change ranks")));

        mockMvc.perform(post("/users/2/grant-permission")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("moderator").roles("MODERATOR"))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .param("permissionId", "1")
                        .param("scopeId", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginPageIsPublicAndPanelRedirectsGuests() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sign in")));

        mockMvc.perform(get("/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void formLoginOpensThePanel() throws Exception {
        var loginResult = mockMvc.perform(formLogin("/login").user("admin").password("Admin123!"))
                .andExpect(authenticated().withUsername("admin"))
                .andReturn();

        mockMvc.perform(get("/users")
                        .session((org.springframework.mock.web.MockHttpSession)
                                loginResult.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("User Management Panel")));
    }

    @Test
    void openApiDocumentIsPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("User Management Panel API"));
    }
}
