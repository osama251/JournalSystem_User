package fullstack2.journalsystem_user;

import fullstack2.journalsystem_user.Models.CreateUserModel;
import fullstack2.journalsystem_user.Models.LocalUser;
import fullstack2.journalsystem_user.service.UserService;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserServiceTest {

    private Keycloak keycloak;                 // admin client (injected)
    private UserService service;

    // chain mocks
    private RealmResource realmResource;
    private UsersResource usersResource;
    private RolesResource rolesResource;
    private UserResource userResource;
    private RoleMappingResource roleMappingResource;
    private RoleScopeResource realmLevelScope;

    @BeforeEach
    void setup() throws Exception {
        keycloak = mock(Keycloak.class);
        service = new UserService(keycloak);

        // inject @Value fields via reflection
        setField(service, "realm", "journal");
        setField(service, "serverUrl", "http://keycloak:8080");
        setField(service, "clientId", "user-mgmt-service");
        setField(service, "clientSecret", "secret");

        realmResource = mock(RealmResource.class);
        usersResource = mock(UsersResource.class);
        rolesResource = mock(RolesResource.class);
        userResource = mock(UserResource.class);
        roleMappingResource = mock(RoleMappingResource.class);
        realmLevelScope = mock(RoleScopeResource.class);

        when(keycloak.realm("journal")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(realmResource.roles()).thenReturn(rolesResource);

        // user roles chain
        when(usersResource.get(anyString())).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(realmLevelScope);
    }

    @Test
    @DisplayName("findUserByUsername returns mapped LocalUser when found")
    void findUserByUsername_returnsLocalUser() {
        UserRepresentation u = new UserRepresentation();
        u.setId("kc-123");
        u.setUsername("doc1@gmail.com");
        u.setEmail("doc1@gmail.com");

        when(usersResource.searchByUsername("doc1@gmail.com", true)).thenReturn(List.of(u));

        RoleRepresentation doctorRole = new RoleRepresentation();
        doctorRole.setName("doctor");
        when(realmLevelScope.listAll()).thenReturn(List.of(doctorRole));

        LocalUser result = service.findUserByUsername("doc1@gmail.com");

        assertNotNull(result);
        assertEquals("kc-123", result.getUserId());
        assertEquals("doc1@gmail.com", result.getUserName());
        assertEquals("doc1@gmail.com", result.getEmail());
        assertEquals("doctor", result.getRole());
    }

    @Test
    @DisplayName("findUserByUsername returns empty-ish LocalUser when user list empty (current behavior: null or empty object)")
    void findUserByUsername_whenNotFound() {
        when(usersResource.searchByUsername("missing", true)).thenReturn(List.of());

        LocalUser result = service.findUserByUsername("missing");

        // Your code prints but returns localUser (default) OR null on exception.
        // With current code it returns LocalUser(), but could be null in catch. Be tolerant:
        assertTrue(result == null || result.getUserId() == null);
    }

    @Test
    @DisplayName("registerNewUser throws when Keycloak create != 201")
    void registerNewUser_throws_onBadStatus() {
        CreateUserModel req = new CreateUserModel("u", "e", "p", "f", "l", "patient");

        Response resp = mock(Response.class);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(resp);
        when(resp.getStatus()).thenReturn(409);

        assertThrows(RuntimeException.class, () -> service.registerNewUser(req));
    }

    @Test
    @DisplayName("verifyAndGetUser returns LocalUser when password flow succeeds")
    void verifyAndGetUser_success() throws Exception {
        // mock KeycloakBuilder.builder() static -> returns builder that builds a Keycloak user client
        Keycloak userClient = mock(Keycloak.class);
        var tokenManager = mock(org.keycloak.admin.client.token.TokenManager.class);
        when(userClient.tokenManager()).thenReturn(tokenManager);
        when(tokenManager.getAccessToken()).thenReturn(new AccessTokenResponse()); // success means password ok

        // search user through admin client after success
        UserRepresentation u = new UserRepresentation();
        u.setId("kc-777");
        u.setUsername("pat1");
        u.setEmail("pat1@x");
        when(usersResource.searchByUsername("pat1", true)).thenReturn(List.of(u));

        RoleRepresentation patientRole = new RoleRepresentation();
        patientRole.setName("patient");
        when(realmLevelScope.listAll()).thenReturn(List.of(patientRole));

        // Static mock KeycloakBuilder
        try (MockedStatic<org.keycloak.admin.client.KeycloakBuilder> mocked = mockStatic(org.keycloak.admin.client.KeycloakBuilder.class)) {
            var builder = mock(org.keycloak.admin.client.KeycloakBuilder.class);

            mocked.when(org.keycloak.admin.client.KeycloakBuilder::builder).thenReturn(builder);

            when(builder.serverUrl(anyString())).thenReturn(builder);
            when(builder.realm(anyString())).thenReturn(builder);
            when(builder.clientId(anyString())).thenReturn(builder);
            when(builder.clientSecret(anyString())).thenReturn(builder);
            when(builder.username(anyString())).thenReturn(builder);
            when(builder.password(anyString())).thenReturn(builder);
            when(builder.grantType(anyString())).thenReturn(builder);
            when(builder.build()).thenReturn(userClient);

            LocalUser result = service.verifyAndGetUser("pat1", "pw");

            assertNotNull(result);
            assertEquals("kc-777", result.getUserId());
            assertEquals("pat1", result.getUserName());
            assertEquals("patient", result.getRole());

            verify(builder).grantType(OAuth2Constants.PASSWORD);
            verify(tokenManager).getAccessToken();
        }
    }

    // ----- helper -----
    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}