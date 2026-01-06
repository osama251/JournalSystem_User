package fullstack2.journalsystem_user.service;

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
import java.util.List;

@Service
public class UserService {

    private final Keycloak keycloak;

    @Value("${KEYCLOAK_REALM}")
    private String realm;
    @Value("${KEYCLOAK_AUTH_SERVER_URL}")
    private String serverUrl;
    @Value("${KEYCLOAK_CLIENT_ID}")
    private String clientId;
    @Value("${KEYCLOAK_CLIENT_SECRET}")
    private String clientSecret;

    public UserService(Keycloak keycloak) {
        this.keycloak = keycloak;
    }

    public LocalUser findUserByUsername(String username) {
        // Query Keycloak for users matching the username
        try{
            List<UserRepresentation> users = keycloak.realm(realm)
                    .users()
                    .searchByUsername(username, true);

            LocalUser localUser = new LocalUser();

            if (users.isEmpty()) {
                System.out.println("No user found with username: " + username);
            } else {

                UserRepresentation user = users.get(0);
                localUser = userRepToLocalUser(user);
            }
            return localUser;
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public LocalUser verifyAndGetUser(String username, String password) {
        // 1. Attempt to authenticate the user directly with their credentials
        try (Keycloak userClient = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .clientId(clientId) // Your user-mgmt-service client ID
                .clientSecret(clientSecret)
                .username(username)
                .password(password)
                .grantType(OAuth2Constants.PASSWORD) // This flow verifies the user's password
                .build()) {

            // 2. Try to get a token. If this fails, it throws an exception (401)
            userClient.tokenManager().getAccessToken();

            // 3. If we reached here, password is valid! Now get the full details using Admin Client
            UserRepresentation user =keycloak.realm(realm)
                    .users()
                    .searchByUsername(username, true)
                    .get(0);
            return userRepToLocalUser(user);
        }
    }

    public void registerNewUser(CreateUserModel request) {
        // 1. Prepare User Representation
        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(true);

        // 2. Prepare Password Credential
        CredentialRepresentation passwordCred = new CredentialRepresentation();
        passwordCred.setTemporary(false);
        passwordCred.setType(CredentialRepresentation.PASSWORD);
        passwordCred.setValue(request.getPassword());
        user.setCredentials(Collections.singletonList(passwordCred));

        // 3. Create User in Keycloak
        Response response = keycloak.realm(realm).users().create(user);

        if (response.getStatus() == 201) {
            // 4. Get the created User ID from the response header
            String userId = CreatedResponseUtil.getCreatedId(response);

            // 5. Assign the Realm Role
            assignRealmRole(userId, request.getRole());
        } else {
            throw new RuntimeException("Keycloak user creation failed with status: " + response.getStatus());
        }
    }

    private void assignRealmRole(String userId, String roleName) {
        // Fetch the role representation from the realm
        RoleRepresentation role = keycloak.realm(realm).roles().get(roleName).toRepresentation();

        // Add the role to the user at the realm level
        keycloak.realm(realm).users().get(userId).roles().realmLevel().add(Collections.singletonList(role));
    }

    private LocalUser userRepToLocalUser(UserRepresentation user){
        List<RoleRepresentation> realmRoles = keycloak.realm(realm)
                .users()
                .get(user.getId())
                .roles()
                .realmLevel()
                .listAll();

        // 2. Extract the names from the RoleRepresentation objects
        List<String> roleNames = realmRoles.stream()
                .map(RoleRepresentation::getName)
                .toList();

        LocalUser localUser = new LocalUser();
        localUser.setUserId(user.getId());
        localUser.setUserName(user.getUsername());
        localUser.setEmail(user.getEmail());

        if (roleNames.contains("doctor")){
            localUser.setRole("doctor");
        } else if (roleNames.contains("patient")) {
            localUser.setRole("patient");
        } else if (roleNames.contains("employee")) {
            localUser.setRole("employee");
        }
        return localUser;
    }
}