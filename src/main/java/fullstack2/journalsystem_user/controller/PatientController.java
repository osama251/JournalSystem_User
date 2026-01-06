package fullstack2.journalsystem_user.controller;

import fullstack2.journalsystem_user.Models.CreatePatientModel;
import fullstack2.journalsystem_user.Models.CreateUserModel;
import fullstack2.journalsystem_user.Models.LocalUser;
import fullstack2.journalsystem_user.service.PatientService;
import fullstack2.journalsystem_user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patient")
public class PatientController {
    PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }


    @PostMapping("/createPatient")
    public CreatePatientModel createPatient(@RequestBody CreatePatientModel request) {
        return patientService.registerPatient(request);
    }

    @GetMapping(path="/getByPatientId/{id}")
    public CreatePatientModel getPatientById(@PathVariable String id) {
        return patientService.getPatientById(id);
    }

    @GetMapping(path="/getByUserName/{userName}")
    public CreatePatientModel getPatientByUserName(@PathVariable String userName) {
        return patientService.getPatientByUsername(userName);
    }

    @GetMapping(path="/getPatientsByDoctorName/{doctorName}")
    public List<CreatePatientModel> getPatientsByDoctorName(@PathVariable String doctorName) {
        return patientService.getPatientsByDoctor(doctorName);
    }

    @PutMapping("/addDoctor/{patientName}/{doctorName}")
    public void addDoctor(@PathVariable String patientName, @PathVariable String doctorName) {
        patientService.addPatient(patientName, doctorName);
    }
}
