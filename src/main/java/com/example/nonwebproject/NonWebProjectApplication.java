package com.example.nonwebproject;

import com.example.nonwebproject.service.HrService;
import com.example.nonwebproject.service.OffStandardService;
import com.example.nonwebproject.service.QadService;
import com.example.nonwebproject.service.UserService;
import com.example.nonwebproject.utility.Utility;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;

import static java.lang.System.exit;

@SpringBootApplication
public class NonWebProjectApplication implements CommandLineRunner {

    //@Autowired
    //private UserService userService;
    @Autowired
    private HrService hrService;

    @Autowired
    private QadService qadService;

    @Autowired
    private OffStandardService offStandardService;

    public static void main(String[] args) throws Exception{
        //disable banner, don't want to see the spring logo
      //  SpringApplication.run(NonWebProjectApplication.class, args);

        SpringApplication app = new SpringApplication(NonWebProjectApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);

    }

    @Override
    public void run(String... args) throws Exception{
        System.out.println("hello world");

        //hrService.runTestCommand();
        //qadService.runTestCommand();
       // offStandardService.runTestCommand();
        //System.out.println(userService.getUsers().get(0).getName());

        String plant_location = "CZ";

       productionRun(plant_location);

        exit(0);
    }

    public void productionRun(String plant_location) throws Exception{
        List<LocalDate> dateRange = Utility.getDatesBetweenUsingJava8("2019-03-15", "2019-03-15");

        for (LocalDate date: dateRange ) {

            String stringDateIn = date.toString();
            hrService.runCommand(stringDateIn,plant_location);
            qadService.runCommand(stringDateIn,plant_location);
            offStandardService.runCommand(stringDateIn,plant_location);

        }
    }
}
