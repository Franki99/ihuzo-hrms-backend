package net.enjoy.springboot.ihuzohr.controller;

import jakarta.validation.Valid;
import net.enjoy.springboot.ihuzohr.dto.ResetPasswordDto;
import net.enjoy.springboot.ihuzohr.dto.UserDto;
import net.enjoy.springboot.ihuzohr.entity.User;
import net.enjoy.springboot.ihuzohr.repository.UserRepository;
import net.enjoy.springboot.ihuzohr.service.EmailService;
import net.enjoy.springboot.ihuzohr.service.UserService;
import net.enjoy.springboot.ihuzohr.service.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.context.Context;

import java.security.Principal;

@Controller
public class AuthController {
    private UserService userService;
    @Autowired
    private EmailService emailService;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserServiceImpl userServiceImpl;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register/save")
    public String registration(@Valid @ModelAttribute("user") UserDto userDto,
                               BindingResult result,
                               Model model) {
        User existingUser = userService.findUserByEmail(userDto.getEmail());

        if (existingUser != null && existingUser.getEmail() != null && !existingUser.getEmail().isEmpty()) {
            result.rejectValue("email", null,
                    "There is already an account registered with the same email");
        }

        if (result.hasErrors()) {
            model.addAttribute("user", userDto);
            return "/register";
        }

//        userService.saveUser(userDto);
        userServiceImpl.saveAdminUser(userDto);
        return "redirect:/dashboard";
    }

    @GetMapping("/users")
    public String users(@RequestParam(value = "search", required = false) String search,
                        @RequestParam(value = "page", defaultValue = "0") int page,
                        @RequestParam(value = "size", defaultValue = "5") int size,
                        @RequestParam(value = "sort", defaultValue = "id") String sort,
                        @RequestParam(value = "dir", defaultValue = "asc") String dir,
                        Model model) {
        if(page < 0) page = 0;
        Pageable pageable = PageRequest.of(page, size, dir.equals("asc") ? Sort.by(sort).ascending() : Sort.by(sort).descending());
        Page<User> userPage;

        if (search != null && !search.isEmpty()) {
            userPage = userServiceImpl.searchUsers(search, pageable);
        } else {
            userPage = userServiceImpl.findAllUsers(pageable);
        }

        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("totalItems", userPage.getTotalElements());
        model.addAttribute("sortField", sort);
        model.addAttribute("sortDir", dir);
        model.addAttribute("reverseSortDir", dir.equals("asc") ? "desc" : "asc");
        model.addAttribute("search", search);
        return "users";
    }

    @PostMapping("/forgot-password")
    public String forgotPwdPost(@ModelAttribute("user") UserDto userDto,
                                Model model){
        try{
        User user = userService.findUserByEmail(userDto.getEmail());
        if(user==null){
            model.addAttribute("messageError","User with this email does not exist");
        }else{
            Context context = new Context();
            context.setVariable("message", "Reset your password by clicking the following http://localhost:8080/reset-password?email="+user.getEmail());
            context.setVariable("recipient",user.getFirstName() + " "+user.getLastName());

            emailService.sendEmail(user.getEmail(),"Forgot Password",context);
            model.addAttribute("messageSuccess","Check your email to reset password");
        }
        } catch (Exception e) {
            model.addAttribute("messageError","Something went wrong: "+e.getMessage());
        }

        return "forgot-password";

    }


    @PostMapping("/reset-password")
    public String resetPwdPost(@ModelAttribute ResetPasswordDto dto,
                               @RequestParam(value = "email") String email,
                                Model model){
        try{
            if(!dto.getPassword().equals(dto.getConfirmPassword())){
                throw new RuntimeException("Password does not match!!");
            }
            User user = userService.findUserByEmail(email);
            if(user==null){
                model.addAttribute("messageError","User with this email does not exist");
            }else{
                user.setPassword(passwordEncoder.encode(dto.getPassword()));
                userRepository.save(user);
                model.addAttribute("messageSuccess","Password has successfully reset");

            }
        } catch (Exception e) {
            model.addAttribute("messageError","Something went wrong: "+e.getMessage());
        }

        return "reset-password";

    }

    @GetMapping("/dashboard")
    public String dashboard( Principal principal,Model model) {
        User user = userService.findUserByEmail(principal.getName());
        model.addAttribute("user", user);
        return "dashboard";
    }

    @GetMapping("/users/edit/{id}")
    public String showUpdateForm(@PathVariable("id") long id, Model model) {
        UserDto user = userService.getUserById(id);
        model.addAttribute("user", user);
        return "edit";
    }

    @PostMapping("/users/update/{id}")
    public String updateUser(@PathVariable("id") long id, @Valid UserDto userDto,
                             BindingResult result, Model model) {
        if (result.hasErrors()) {
            userDto.setId(id);
            result.getAllErrors().forEach(error -> System.out.println("Hello World: "+error.toString()));
            return "redirect:/users/edit/"+id;
        }
        User user = userService.findUserByEmail(userDto.getEmail());
        if(user==null){
            return "redirect:/users/edit/"+id;
        }
        userDto.setDateOfBirth(user.getDateOfBirth());
        userService.updateUser(userDto);
        return "redirect:/users";
    }

    @GetMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable("id") long id, RedirectAttributes redirectAttributes) {
        try{
           userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("messageSuccess","User deleted successfully");
        } catch (Exception e){
            redirectAttributes.addFlashAttribute("messageError","Something went wrong: "+e.getMessage());
        }
        return "redirect:/users";
    }
}