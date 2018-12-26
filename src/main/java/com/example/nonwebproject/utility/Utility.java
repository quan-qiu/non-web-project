package com.example.nonwebproject.utility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Utility {


    public static String mytoString(String[] theArray, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < theArray.length; i++) {
            if (i > 0) {
                sb.append(delimiter);
            }
            String item = theArray[i];
            sb.append(item);
        }
        return sb.toString();
    }

    public static void writeToFile(String fileName, List context) {
        String outFile = createOutFileName(fileName);

        try {
            Files.write(Paths.get(outFile), context, StandardOpenOption.APPEND);
        } catch (IOException e) {
        }

    }

    public static void deleteFile(String fileName) {
        String outFile = createOutFileName(fileName);

        Path out = Paths.get(outFile);
        try {
            boolean result = Files.deleteIfExists(out);
            List<String> firstLine = new ArrayList<>();
            firstLine.add(" List VTYC_PN without having labor minutes.");
            firstLine.add("");
            Files.write(Paths.get(outFile), firstLine, StandardOpenOption.CREATE);
        } catch (IOException e) {

        }

    }

    public static String createOutFileName(String fileName) {

        String outFileName = "d:\\out\\out-" + fileName + ".txt";

        return outFileName;
    }

    public static Map generateShiftDate(String s_date, String shift_type) {
        Map<String, Object> result = new HashMap();
        String shift_start = "";
        String shift_end = "";

        switch (shift_type) {
            case "day":
                shift_start = s_date + " 08:00:00";
                shift_end = s_date + " 20:00:00";
                break;
            case "night":
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDate tomorrow = LocalDate.from(LocalDate.parse(s_date, formatter)).plusDays(1);
                DateTimeFormatter t_formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                String s_tomorrow = tomorrow.format(t_formatter);
                shift_start = s_date + " 20:00:00";
                shift_end = s_tomorrow + " 08:00:00";
                break;
            default:
                break;
        }

        result.put("shift_start", shift_start);
        result.put("shift_end", shift_end);

        return result;
    }

    public static String getCostcenterByWorkcenter(String PROLINK_API_SERVER, String workcenter, String plant_location) throws Exception{
        if (workcenter.equals("")){
            return "";
        }
        String costcenter=""  ;
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();

        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        String contentType = "application/json;charset=UTF-8";
        MediaType mediaType = new MediaType("application", "json", Charset.forName("UTF-8"));
        headers.setContentType(mediaType);
        String resourceURL = PROLINK_API_SERVER + plant_location + "/workcenterToCostcenter/" + workcenter;

        HttpEntity<String> entity = new HttpEntity<String>(headers);

        ResponseEntity<String> response = restTemplate.exchange(resourceURL, HttpMethod.GET, entity, String.class);
        HashMap dlWorkHour = new HashMap();
        if (null != response || response.getStatusCode() == HttpStatus.OK) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode;
            rootNode = objectMapper.readTree(response.getBody());

            JsonNode dataNode = rootNode.path("data");
            Iterator<JsonNode> elements = dataNode.elements();

            while (elements.hasNext()) {
                JsonNode idNode = elements.next();
                costcenter = idNode.get("costcenter").textValue();
            }
        }
        System.out.println(costcenter);

        return costcenter;
    }

    public static List<LocalDate> getDatesBetweenUsingJava8(
            String start, String end) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-d");

        //convert String to LocalDate
        LocalDate startLocalDate = LocalDate.parse(start, formatter);
        LocalDate endLocalDate = LocalDate.parse(end, formatter);

        long numOfDaysBetween = ChronoUnit.DAYS.between(startLocalDate, endLocalDate) + 1;
        return IntStream.iterate(0, i -> i + 1)
                .limit(numOfDaysBetween)
                .mapToObj(i -> startLocalDate.plusDays(i))
                .collect(Collectors.toList());
    }
}
