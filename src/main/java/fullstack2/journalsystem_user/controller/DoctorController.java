package fullstack2.journalsystem_user.controller;

import fullstack2.journalsystem_user.Models.CreateDoctorModel;
import fullstack2.journalsystem_user.Models.CreateUserModel;
import fullstack2.journalsystem_user.Models.LocalUser;
import fullstack2.journalsystem_user.service.DoctorService;
import fullstack2.journalsystem_user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/doctor")
public class DoctorController {

    private final DoctorService doctorService;
    private final UserService userService;
    public DoctorController(DoctorService doctorService,  UserService userService) {
        this.doctorService = doctorService;
        this.userService = userService;
    }

    @PostMapping("/createDoctor")
    public LocalUser createDoctor(@RequestBody CreateDoctorModel request) {
        try {
            doctorService.registerDoctor(request);
            return userService.findUserByUsername(request.getUsername());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    @GetMapping("/getDoctorByDoctorId/{doctorId}")
    public CreateDoctorModel getDoctorByDoctorId(@PathVariable String doctorId) {
        return doctorService.getDoctorById(doctorId);
    }

    @GetMapping("/getDoctorByUserName/{userName}")
    public CreateDoctorModel getDoctorByUserName(@PathVariable("userName") String username) {
        return doctorService.getDoctorByUsername(username);
    }

    @GetMapping("/getDoctorsByOrganizationName/{organizationName}")
    public List<CreateDoctorModel> getDoctorsByOrganizationName(@PathVariable("organizationName") String organizationName) {
        List<CreateDoctorModel> doctors = doctorService.getDoctorsByOrganization(organizationName);
        if (doctors.isEmpty()) {
            return null;
        }
        return doctors;
    }
}
