package com.example.nonwebproject;

import com.example.nonwebproject.service.*;
import com.example.nonwebproject.utility.Utility;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.System.exit;
import static java.time.temporal.ChronoUnit.DAYS;

@SpringBootApplication
@EnableScheduling
public class NonWebProjectApplication implements CommandLineRunner {

    //@Autowired
    //private UserService userService;
    @Autowired
    private HrService hrService;

    @Autowired
    private QadService qadService;

    @Autowired
    private OffStandardService offStandardService;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private DlmsService dlmsService;

    public static void main(String[] args) throws Exception{
        //disable banner, don't want to see the spring logo
      //  SpringApplication.run(NonWebProjectApplication.class, args);

        SpringApplication app = new SpringApplication(NonWebProjectApplication.class);
        app.setBannerMode(Banner.Mode.OFF);

        System.out.println(Arrays.asList(args));
        /*int args_len = args.length;

        String[] in_date= new String[2];
        switch (args_len){
            case 0:
                System.out.println("Running format should be like one of below styles. \n" +
                        " Ex: \n calculate yesterday's off-standard report : java -jar non-wen-project.jar -d ;\n " +
                        "calculate off-standard report for a particular date: java -jar non-web-project.jar -d:2019-04-28 ;\n " +
                        "calculate off-standard report for a period: java -jar non-web-project.jar -d:2019-04-28,2019-05-03 ");
                break;

            case 1:
                String s_date = args[0];
                String[] argArr = Arrays.asList(args[0].split(":"))
                        .stream()
                        .map(String::trim)
                        .toArray(String[]::new);

                System.out.println(Arrays.asList(argArr));
                if (argArr[0].equals("-d")){
                    if(argArr.length == 1){
                        System.out.println("*****************************"+ argArr.length);
                        app.run(argArr);
                    }
                    if(argArr.length > 1){
                        System.out.println("------------------------------");
                        String[] dateArr = Arrays.asList(argArr[1].split(","))
                                .stream()
                                .map(String::trim)
                                .toArray(String[]::new);

                        int len = dateArr.length;
                        System.out.println("all:" + Arrays.asList(dateArr));
                        System.out.println(len);
                        if (len == 1){
                            String[] arr = {dateArr[0],dateArr[0]};

                            System.out.println("1:" + Arrays.asList(arr));
                            app.run(arr);
                        }else if (len== 2){
                            String[] arr = {dateArr[0],dateArr[1]};
                            System.out.println("==============================");
                            System.out.println("2:" + Arrays.asList(arr));

                            app.run(arr);
                        }

                    }

                }
                break;
        }*/

       app.run(args);

       // taskScheduler.scheduleWithFixedDelay(app.run(args), ONE_SECOND);

    }

    @Override
    public void run(String... args) throws Exception{
        //System.out.println("hello world");
        System.out.println(Arrays.asList(args));
        /*if(args[0].equals("-d")){

            String plant_location = "CZ";
            productionRun(plant_location,null,null);

            plant_location = "CQ";
            productionRun(plant_location,null,null);
        }else{
            System.out.println(Arrays.asList(args));
            String plant_location = "CZ";
            productionRun(plant_location,args[0],args[1]);

            plant_location = "CQ";
            productionRun(plant_location,args[0],args[1]);
        }*/

        //hrService.runTestCommand();
        //qadService.runTestCommand();
        //offStandardService.runTestCommand();
        //dlmsService.operation();
        //System.out.println(userService.getUsers().get(0).getName());

        String plant_location = "CZ";
        productionRun(plant_location,"2019-06-21","2019-06-22");

        plant_location = "CQ";
        productionRun(plant_location,"2019-06-21","2019-06-22");

/*        String plant_location = "CZ";
        productionRun(plant_location);
        plant_location = "CQ";
        productionRun(plant_location);*/

        System.out.print("******************************************************************************************\n");
        System.out.print("*                                    All finished                                        *\n");
        System.out.print("******************************************************************************************\n");
        exit(0);

        //scheduleRun();
    }



/*    @Scheduled(fixedRate = 86400)
    public void scheduleRun() throws Exception{
        String plant_location = "CZ";
        System.out.println(plant_location);
        productionRun(plant_location);

        plant_location = "CQ";
        System.out.println(plant_location);
        productionRun(plant_location);
    }*/

    public void productionRun(String plant_location,String start_date,String end_date) throws Exception{

        List<LocalDate> dateRange = null;

        if (start_date == "1"){
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minus(1, DAYS);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            System.out.println(yesterday.format(formatter));

            dateRange = Utility.getDatesBetweenUsingJava8(yesterday.format(formatter), yesterday.format(formatter));
        }else{
            dateRange = Utility.getDatesBetweenUsingJava8(start_date, end_date);
        }
        /*LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minus(1, DAYS);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        System.out.println(yesterday.format(formatter));
        System.out.println(today.format(formatter));
        List<LocalDate> dateRange = Utility.getDatesBetweenUsingJava8("2019-04-27", "2019-04-27");
        //List<LocalDate> dateRange = Utility.getDatesBetweenUsingJava8(yesterday.format(formatter), yesterday.format(formatter));*/
        System.out.print(Arrays.asList(dateRange));
        for (LocalDate date: dateRange ) {

            String stringDateIn = date.toString();
            hrService.runCommand(stringDateIn,plant_location);
            qadService.runCommand(stringDateIn,plant_location);
            offStandardService.runCommand(stringDateIn,plant_location);

        }
    }


    public void productionRun(String plant_location) throws Exception{

        List<LocalDate> dateRange = null;

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minus(1, DAYS);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        System.out.println(yesterday.format(formatter));
        dateRange = Utility.getDatesBetweenUsingJava8(yesterday.format(formatter), yesterday.format(formatter));

        System.out.print(Arrays.asList(dateRange));
        for (LocalDate date: dateRange ) {
            String stringDateIn = date.toString();
            hrService.runCommand(stringDateIn,plant_location);
            qadService.runCommand(stringDateIn,plant_location);
            offStandardService.runCommand(stringDateIn,plant_location);

        }
    }

}
