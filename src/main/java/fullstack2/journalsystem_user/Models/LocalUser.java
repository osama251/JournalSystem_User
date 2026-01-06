package fullstack2.journalsystem_user.Models;

public class LocalUser {

    private String userId;
    private String userName;
    private String email;
    private String role;

    public LocalUser(String userId, String email, String role) {
        this.userId = userId;
        this.email = email;
        this.role = role;
    }

    public LocalUser(){
        this.userId = null;
        this.email = null;
        this.role = null;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }
    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    @Override
    public String toString() {
        return "LocalUser{" +
                "id='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", Role='" + role + '\'' +
                '}';
    }
}
