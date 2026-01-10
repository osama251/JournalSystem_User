package fullstack2.journalsystem_user;

import fullstack2.journalsystem_user.Models.CreateDoctorModel;
import fullstack2.journalsystem_user.service.DoctorService;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DoctorServiceTest {

    private Keycloak keycloak;
    private DoctorService service;

    private RealmResource realm;
    private UsersResource users;
    private RolesResource roles;
    private UserResource userResource;
    private RoleMappingResource roleMapping;
    private RoleScopeResource realmLevel;

    @BeforeEach
    void setup() throws Exception {
        keycloak = mock(Keycloak.class);
        service = new DoctorService(keycloak);

        setField(service, "realm", "journal");
        setField(service, "serverUrl", "http://keycloak:8080");
        setField(service, "clientId", "user-mgmt-service");
        setField(service, "clientSecret", "secret");

        realm = mock(RealmResource.class);
        users = mock(UsersResource.class);
        roles = mock(RolesResource.class);
        userResource = mock(UserResource.class);
        roleMapping = mock(RoleMappingResource.class);
        realmLevel = mock(RoleScopeResource.class);

        when(keycloak.realm("journal")).thenReturn(realm);
        when(realm.users()).thenReturn(users);
        when(realm.roles()).thenReturn(roles);

        when(users.get(anyString())).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMapping);
        when(roleMapping.realmLevel()).thenReturn(realmLevel);
    }

    @Test
    @DisplayName("registerDoctor throws when status != 201")
    void registerDoctor_throws() {
        CreateDoctorModel req = new CreateDoctorModel("doc1", "doc1@x", "pw", "A", "B", "doctor", "Org", "Addr");

        Response resp = mock(Response.class);
        when(users.create(any(UserRepresentation.class))).thenReturn(resp);
        when(resp.getStatus()).thenReturn(409);

        assertThrows(RuntimeException.class, () -> service.registerDoctor(req));
    }

    @Test
    @DisplayName("getDoctorByUsername maps org attributes and role")
    void getDoctorByUsername_maps() {
        UserRepresentation brief = new UserRepresentation();
        brief.setId("kc-doc");

        when(users.searchByUsername("doc1", true)).thenReturn(List.of(brief));

        UserRepresentation full = new UserRepresentation();
        full.setId("kc-doc");
        full.setUsername("doc1");
        full.setEmail("doc1@x");
        full.setFirstName("A");
        full.setLastName("B");
        full.setAttributes(Map.of(
                "organizationName", List.of("Org"),
                "organizationAddress", List.of("Addr")
        ));

        when(users.get("kc-doc")).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(full);

        RoleRepresentation doctorRole = new RoleRepresentation();
        doctorRole.setName("doctor");
        when(realmLevel.listAll()).thenReturn(List.of(doctorRole));

        CreateDoctorModel result = service.getDoctorByUsername("doc1");

        assertNotNull(result);
        assertEquals("doc1", result.getUsername());
        assertEquals("doc1@x", result.getEmail());
        assertEquals("Org", result.getOrganizationName());
        assertEquals("Addr", result.getOrganizationAddress());
        assertEquals("doctor", result.getRole());
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}