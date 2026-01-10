package fullstack2.journalsystem_user;

import fullstack2.journalsystem_user.Models.CreatePatientModel;
import fullstack2.journalsystem_user.service.PatientService;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PatientServiceTest {

    private Keycloak keycloak;
    private PatientService service;

    private RealmResource realm;
    private UsersResource users;
    private RolesResource roles;

    @BeforeEach
    void setup() throws Exception {
        keycloak = mock(Keycloak.class);
        service = new PatientService(keycloak);
        setField(service, "realm", "journal");
        setField(service, "serverUrl", "http://keycloak:8080");
        setField(service, "clientId", "user-mgmt-service");
        setField(service, "clientSecret", "secret");

        realm = mock(RealmResource.class);
        users = mock(UsersResource.class);
        roles = mock(RolesResource.class);

        when(keycloak.realm("journal")).thenReturn(realm);
        when(realm.users()).thenReturn(users);
        when(realm.roles()).thenReturn(roles);
    }

    @Test
    @DisplayName("getPatientByUsername maps attributes")
    void getPatientByUsername_mapsAttrs() {
        UserRepresentation brief = new UserRepresentation();
        brief.setId("kc-p1");

        when(users.searchByUsername("pat1", true)).thenReturn(List.of(brief));

        UserResource userRes = mock(UserResource.class);
        when(users.get("kc-p1")).thenReturn(userRes);

        UserRepresentation full = new UserRepresentation();
        full.setId("kc-p1");
        full.setUsername("pat1");
        full.setEmail("pat1@x");
        full.setFirstName("A");
        full.setLastName("B");
        full.setAttributes(Map.of(
                "telephoneNr", List.of("070"),
                "address", List.of("Street 1"),
                "gender", List.of("male"),
                "age", List.of("22")
        ));
        when(userRes.toRepresentation()).thenReturn(full);

        CreatePatientModel result = service.getPatientByUsername("pat1");

        assertNotNull(result);
        assertEquals("pat1", result.getUsername());
        assertEquals("pat1@x", result.getEmail());
        assertEquals("Street 1", result.getAddress());
        assertEquals("070", result.getTelephoneNr());
        assertEquals("male", result.getGender());
        assertEquals(22, result.getAge());
    }

    @Test
    @DisplayName("registerPatient throws when create != 201")
    void registerPatient_throws() {
        CreatePatientModel req = new CreatePatientModel("u","e","p","f","l","patient","070","addr",30,"male");
        Response resp = mock(Response.class);
        when(users.create(any(UserRepresentation.class))).thenReturn(resp);
        when(resp.getStatus()).thenReturn(409);

        assertThrows(RuntimeException.class, () -> service.registerPatient(req));
    }

    @Test
    @DisplayName("addPatient adds username into doctor's patients attribute (no duplicate)")
    void addPatient_updatesDoctorAttribute() {
        UserRepresentation doctorBrief = new UserRepresentation();
        doctorBrief.setId("kc-doc");

        when(users.searchByUsername("doc", true)).thenReturn(List.of(doctorBrief));

        UserResource docRes = mock(UserResource.class);
        when(users.get("kc-doc")).thenReturn(docRes);

        UserRepresentation docFull = new UserRepresentation();
        docFull.setId("kc-doc");
        docFull.setAttributes(new HashMap<>()); // empty
        when(docRes.toRepresentation()).thenReturn(docFull);

        service.addPatient("pat1", "doc");

        verify(docRes, times(1)).update(argThat(updated -> {
            List<String> pts = updated.getAttributes().get("patients");
            return pts != null && pts.size() == 1 && pts.contains("pat1");
        }));

        // call again should not duplicate
        when(docRes.toRepresentation()).thenReturn(docFull); // now already has it
        service.addPatient("pat1", "doc");

        verify(docRes, times(1)).update(any()); // still only once
    }

    // helper
    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}