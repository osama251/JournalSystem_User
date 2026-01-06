package fullstack2.journalsystem_user.service;

import fullstack2.journalsystem_user.Models.CreateDoctorModel;
import fullstack2.journalsystem_user.Models.CreateUserModel;
import fullstack2.journalsystem_user.Models.LocalUser;
import jakarta.ws.rs.core.Response;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DoctorService {
    private final Keycloak keycloak;

    @Value("${KEYCLOAK_REALM}")
    private String realm;
    @Value("${KEYCLOAK_AUTH_SERVER_URL}")
    private String serverUrl;
    @Value("${KEYCLOAK_CLIENT_ID}")
    private String clientId;
    @Value("${KEYCLOAK_CLIENT_SECRET}")
    private String clientSecret;

    public DoctorService(Keycloak keycloak) {
        this.keycloak = keycloak;
    }

    public void registerDoctor(CreateDoctorModel request) {
        // 1. Prepare User Representation
        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(true);

        // 2. Prepare Custom Attributes (Organization Info)
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("organizationName", Collections.singletonList(request.getOrganizationName()));
        attributes.put("organizationAddress", Collections.singletonList(request.getOrganizationAddress()));
        user.setAttributes(attributes);

        // 3. Prepare Password Credential
        CredentialRepresentation passwordCred = new CredentialRepresentation();
        passwordCred.setTemporary(false);
        passwordCred.setType(CredentialRepresentation.PASSWORD);
        passwordCred.setValue(request.getPassword());
        user.setCredentials(Collections.singletonList(passwordCred));

        // 4. Create User in Keycloak
        Response response = keycloak.realm(realm).users().create(user);

        if (response.getStatus() == 201) {
            // 5. Get the created User ID from the response header
            String userId = CreatedResponseUtil.getCreatedId(response);

            // 6. Assign the Realm Role (e.g., "doctor")
            assignRealmRole(userId, request.getRole());

            System.out.println("Doctor registered successfully with ID: " + userId);
        } else {
            // If status is 409, it usually means user already exists
            throw new RuntimeException("Keycloak doctor creation failed with status: " + response.getStatus());
        }
    }

    private void assignRealmRole(String userId, String roleName) {
        // Fetch the role representation from the realm
        RoleRepresentation role = keycloak.realm(realm).roles().get(roleName).toRepresentation();

        // Add the role to the user at the realm level
        keycloak.realm(realm).users().get(userId).roles().realmLevel().add(Collections.singletonList(role));
    }

    public CreateDoctorModel getDoctorById(String userId) {
        // 1. Fetch the user directly by their unique ID
        // This returns the full representation including attributes
        UserRepresentation userRep;
        try {
            userRep = keycloak.realm(realm)
                    .users()
                    .get(userId)
                    .toRepresentation();
        } catch (Error e) {
            throw new RuntimeException("Doctor not found with ID: " + userId);
        }

        // 2. Extract the attributes from the map
        Map<String, List<String>> attributes = userRep.getAttributes();

        String orgName = getAttributeValue(attributes, "organizationName");
        String orgAddress = getAttributeValue(attributes, "organizationAddress");

        // 3. Optional: Fetch the actual role from Keycloak
        List<RoleRepresentation> roles = keycloak.realm(realm)
                .users()
                .get(userId)
                .roles()
                .realmLevel()
                .listAll();

        String roleName = roles.stream()
                .map(RoleRepresentation::getName)
                .filter(name -> name.equalsIgnoreCase("doctor"))
                .findFirst()
                .orElse("doctor");

        // 4. Return the populated model
        return new CreateDoctorModel(
                userRep.getUsername(),
                userRep.getEmail(),
                null, // Password remains null for GET requests
                userRep.getFirstName(),
                userRep.getLastName(),
                roleName,
                orgName,
                orgAddress
        );
    }

    public CreateDoctorModel getDoctorByUsername(String username) {
        // 1. Search for the user by username
        List<UserRepresentation> users = keycloak.realm(realm)
                .users()
                .searchByUsername(username, true);

        if (users.isEmpty()) {
            throw new RuntimeException("Doctor not found with username: " + username);
        }

        UserRepresentation userRep = keycloak.realm(realm)
                .users()
                .get(users.get(0).getId())
                .toRepresentation();

        Map<String, List<String>> attributes = userRep.getAttributes();

        // 2. Extract Custom Attributes safely
        String orgName = getAttributeValue(attributes, "organizationName");
        String orgAddress = getAttributeValue(attributes, "organizationAddress");

        // 3. Fetch the Role (optional: assumes they have one primary realm role)
        List<RoleRepresentation> roles = keycloak.realm(realm)
                .users()
                .get(userRep.getId())
                .roles()
                .realmLevel()
                .listAll();

        String roleName = roles.isEmpty() ? "doctor" : roles.get(0).getName();

        // 4. Initialize the Model (setting password to null as requested)
        return new CreateDoctorModel(
                userRep.getUsername(),
                userRep.getEmail(),
                null, // Password set to null
                userRep.getFirstName(),
                userRep.getLastName(),
                roleName,
                orgName,
                orgAddress
        );
    }

    public List<CreateDoctorModel> getDoctorsByOrganization(String orgName) {
        // 1. Search users by attribute
        List<UserRepresentation> briefUsers = keycloak.realm(realm)
                .users()
                .searchByAttributes("organizationName:" + orgName);

        return briefUsers.stream()
                .filter(briefUser -> {
                    // 2. Check if the user has the "doctor" role
                    List<RoleRepresentation> roles = keycloak.realm(realm)
                            .users()
                            .get(briefUser.getId())
                            .roles()
                            .realmLevel()
                            .listAll();

                    return roles.stream()
                            .anyMatch(role -> role.getName().equalsIgnoreCase("doctor"));
                })
                .map(briefUser -> {
                    // 3. Fetch full representation for the attributes
                    UserRepresentation fullUser = keycloak.realm(realm)
                            .users()
                            .get(briefUser.getId())
                            .toRepresentation();

                    Map<String, List<String>> attributes = fullUser.getAttributes();

                    return new CreateDoctorModel(
                            fullUser.getUsername(),
                            fullUser.getEmail(),
                            null,
                            fullUser.getFirstName(),
                            fullUser.getLastName(),
                            "doctor",
                            getAttributeValue(attributes, "organizationName"),
                            getAttributeValue(attributes, "organizationAddress")
                    );
                })
                .collect(Collectors.toList());
    }

    // Helper method to handle null checks for attributes
    private String getAttributeValue(Map<String, List<String>> attributes, String key) {
        if (attributes != null && attributes.containsKey(key) && !attributes.get(key).isEmpty()) {
            return attributes.get(key).get(0);
        }
        return null;
    }


}
