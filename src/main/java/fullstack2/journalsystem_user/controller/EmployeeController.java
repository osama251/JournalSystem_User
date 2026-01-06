package fullstack2.journalsystem_user.controller;

import fullstack2.journalsystem_user.Models.CreateDoctorModel;
import fullstack2.journalsystem_user.Models.CreateEmployeeModel;
import fullstack2.journalsystem_user.Models.CreateUserModel;
import fullstack2.journalsystem_user.Models.LocalUser;
import fullstack2.journalsystem_user.service.DoctorService;
import fullstack2.journalsystem_user.service.EmployeeService;
import fullstack2.journalsystem_user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.print.Doc;
import java.util.List;

@RestController
@RequestMapping("/api/employee")
public class EmployeeController {

    DoctorService doctorService;
    UserService userService;
    EmployeeService employeeService;
    public EmployeeController(DoctorService doctorService, UserService userService, EmployeeService employeeService) {
        this.doctorService = doctorService;
        this.userService = userService;
        this.employeeService = employeeService;
    }

    @PostMapping("/createEmployee")
    public CreateEmployeeModel createEmployee(@RequestBody CreateEmployeeModel request) {
        try {
            employeeService.registerEmployee(request);
            return employeeService.getEmployeeByUsername(request.getUsername());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    @GetMapping(path="/getEmployeeById/{id}")
    public CreateEmployeeModel getEmployeeById(@PathVariable String id){
        return employeeService.getEmployeeById(id);
    }

    @GetMapping(path="/getEmployeeByUserName/{userName}")
    public CreateEmployeeModel getEmployeeByUserName(@PathVariable String userName){
        return employeeService.getEmployeeByUsername(userName);
    }

    @GetMapping(path="/getEmployeesByOrganization/{orgName}")
    public List<CreateEmployeeModel> getEmployeesByOrganization(@PathVariable String orgName){
        return employeeService.getEmployeesByOrganization(orgName);
    }

}
