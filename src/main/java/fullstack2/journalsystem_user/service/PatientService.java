package fullstack2.journalsystem_user.service;

import fullstack2.journalsystem_user.Models.CreatePatientModel;
import fullstack2.journalsystem_user.Models.CreateUserModel;
import fullstack2.journalsystem_user.Models.LocalUser;
import jakarta.ws.rs.core.Response;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PatientService {
    private final Keycloak keycloak;

    @Value("${KEYCLOAK_REALM}")
    private String realm;
    @Value("${KEYCLOAK_AUTH_SERVER_URL}")
    private String serverUrl;
    @Value("${KEYCLOAK_CLIENT_ID}")
    private String clientId;
    @Value("${KEYCLOAK_CLIENT_SECRET}")
    private String clientSecret;

    public PatientService(Keycloak keycloak) {
        this.keycloak = keycloak;
    }

    public CreatePatientModel getPatientById(String userId) {
        UserRepresentation userRep = keycloak.realm(realm).users().get(userId).toRepresentation();
        Map<String, List<String>> attrs = userRep.getAttributes();

        // Safely parse age, defaulting to 0 if missing or invalid
        String ageStr = getAttributeValue(attrs, "age");
        int age = (ageStr != null) ? Integer.parseInt(ageStr) : 0;

        return new CreatePatientModel(
                userRep.getUsername(),
                userRep.getEmail(),
                null, // Password set to null for safety
                userRep.getFirstName(),
                userRep.getLastName(),
                "patient",
                getAttributeValue(attrs, "telephoneNr"),
                getAttributeValue(attrs, "address"),
                age,
                getAttributeValue(attrs, "gender")
        );
    }

    public CreatePatientModel getPatientByUsername(String username) {
        // 1. Search for the user by username
        List<UserRepresentation> users = keycloak.realm(realm)
                .users()
                .searchByUsername(username, true);

        if (users.isEmpty()) {
            throw new RuntimeException("Patient not found with username: " + username);
        }

        // 2. Fetch the full representation to ensure attributes are included
        UserRepresentation userRep = keycloak.realm(realm)
                .users()
                .get(users.get(0).getId())
                .toRepresentation();

        Map<String, List<String>> attrs = userRep.getAttributes();

        // 3. Convert age from String to int safely
        String ageStr = getAttributeValue(attrs, "age");
        int age = (ageStr != null) ? Integer.parseInt(ageStr) : 0;

        // 4. Return the populated model (password null for security)
        return new CreatePatientModel(
                userRep.getUsername(),
                userRep.getEmail(),
                null,
                userRep.getFirstName(),
                userRep.getLastName(),
                "patient",
                getAttributeValue(attrs, "telephoneNr"),
                getAttributeValue(attrs, "address"),
                age,
                getAttributeValue(attrs, "gender")
        );
    }

    public CreatePatientModel registerPatient(CreatePatientModel request) {
        // 1. Prepare User Representation
        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(true);

        // 2. Map Patient Attributes (Converting age to String)
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("telephoneNr", Collections.singletonList(request.getTelephoneNr()));
        attributes.put("address", Collections.singletonList(request.getAddress()));
        attributes.put("gender", Collections.singletonList(request.getGender()));
        attributes.put("age", Collections.singletonList(String.valueOf(request.getAge())));
        user.setAttributes(attributes);

        // 3. Set Password
        CredentialRepresentation passwordCred = new CredentialRepresentation();
        passwordCred.setTemporary(false);
        passwordCred.setType(CredentialRepresentation.PASSWORD);
        passwordCred.setValue(request.getPassword());
        user.setCredentials(Collections.singletonList(passwordCred));

        // 4. Create in Keycloak
        Response response = keycloak.realm(realm).users().create(user);

        if (response.getStatus() == 201) {
            String userId = CreatedResponseUtil.getCreatedId(response);

            // 5. Assign Role
            assignRealmRole(userId, request.getRole());

            // 6. Fetch and return the registered patient (using ID for accuracy)
            return getPatientById(userId);
        } else {
            throw new RuntimeException("Patient registration failed. Status: " + response.getStatus());
        }
    }

    private String getAttributeValue(Map<String, List<String>> attributes, String key) {
        if (attributes != null && attributes.containsKey(key) && !attributes.get(key).isEmpty()) {
            return attributes.get(key).get(0);
        }
        return null;
    }

    private void assignRealmRole(String userId, String roleName) {
        // Fetch the role representation from the realm
        RoleRepresentation role = keycloak.realm(realm).roles().get(roleName).toRepresentation();

        // Add the role to the user at the realm level
        keycloak.realm(realm).users().get(userId).roles().realmLevel().add(Collections.singletonList(role));
    }

    public void addPatient(String patientUsername, String doctorUsername) {
        // 1. Find the doctor by username to get their ID
        List<UserRepresentation> doctors = keycloak.realm(realm)
                .users()
                .searchByUsername(doctorUsername, true);

        if (doctors.isEmpty()) {
            throw new RuntimeException("Doctor not found: " + doctorUsername);
        }

        String doctorId = doctors.get(0).getId();
        UserResource doctorResource = keycloak.realm(realm).users().get(doctorId);
        UserRepresentation doctorRep = doctorResource.toRepresentation();

        // 2. Get current attributes or create new map if null
        Map<String, List<String>> attributes = doctorRep.getAttributes();
        if (attributes == null) {
            attributes = new HashMap<>();
        }

        // 3. Update the 'patients' attribute list
        List<String> patientList = attributes.getOrDefault("patients", new ArrayList<>());

        // Check if patient is already assigned to avoid duplicates
        if (!patientList.contains(patientUsername)) {
            patientList.add(patientUsername);
            attributes.put("patients", patientList);
            doctorRep.setAttributes(attributes);

            // 4. Push the update to Keycloak
            doctorResource.update(doctorRep);
            System.out.println("DEBUG: Patient " + patientUsername + " added to Doctor " + doctorUsername);
        } else {
            System.out.println("DEBUG: Patient already assigned to this doctor.");
        }
    }

    public List<CreatePatientModel> getPatientsByDoctor(String doctorUsername) {
        // 1. Find the doctor to get their attributes
        List<UserRepresentation> doctors = keycloak.realm(realm)
                .users()
                .searchByUsername(doctorUsername, true);

        if (doctors.isEmpty()) {
            throw new RuntimeException("Doctor not found: " + doctorUsername);
        }

        // Get full representation to ensure attributes are loaded
        UserRepresentation doctorRep = keycloak.realm(realm)
                .users()
                .get(doctors.get(0).getId())
                .toRepresentation();

        Map<String, List<String>> attributes = doctorRep.getAttributes();

        // 2. Extract the list of patient usernames
        List<String> patientUsernames = (attributes != null) ? attributes.get("patients") : null;

        if (patientUsernames == null || patientUsernames.isEmpty()) {
            return new ArrayList<>(); // Return empty list if no patients assigned
        }

        // 3. Fetch each patient's full model
        return patientUsernames.stream()
                .map(username -> {
                    try {
                        // Reuse your existing logic to find and map the patient
                        List<UserRepresentation> foundPatients = keycloak.realm(realm)
                                .users()
                                .searchByUsername(username, true);

                        if (foundPatients.isEmpty()) return null;

                        // Call your existing getPatientById using the ID found
                        return getPatientById(foundPatients.get(0).getId());
                    } catch (Exception e) {
                        System.err.println("Error fetching patient " + username + ": " + e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull) // Remove any patients that weren't found
                .collect(Collectors.toList());
    }

}
