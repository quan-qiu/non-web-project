package com.example.nonwebproject.service;

import com.example.nonwebproject.utility.Utility;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.*;

@Service
public class HrService {

    @Qualifier("jdbcPrimaryTemplate")
    @Autowired
    JdbcTemplate jdbcPrimaryTemplate;

    @Value("${DLMS_API_SERVER}")
    private String DLMS_API_SERVER;

    @Value("${PROLINK_API_SERVER}")
    private String PROLINK_API_SERVER;

    public void runTestCommand() {
        String s_date = "2019-03-18";
        String plant_location = "CZ";

        try {
            deleteExistDlmsHrWorkHour(s_date, plant_location);

            //List<String> ids = getEmployeeIdsFromHr(s_date,plant_location);
            List<String> ids = getEmployeeIdsFromDlms(s_date,plant_location);
            for (String id : ids){
                insertDlmsHrWorkHour(getDlWorkHoursFromHr(id,s_date,plant_location));
            }
        }catch (Exception e){
            System.out.println(e.fillInStackTrace());
        }
    }

    public void runCommand(String s_date, String plant_location) {

        try {
            deleteExistDlmsHrWorkHour(s_date, plant_location);
            //List<String> ids = getEmployeeIdsFromHr(s_date,plant_location);
            List<String> ids = getEmployeeIdsFromDlms(s_date,plant_location);
            for (String id : ids){
                insertDlmsHrWorkHour(getDlWorkHoursFromHr(id,s_date, plant_location));
            }

        }catch (Exception e){
            System.out.println(e.fillInStackTrace());
        }
    }

    public List<String> getEmployeeIdsFromHr(String s_date, String plant_location) throws Exception{
        List<String> employeeIds = new ArrayList<>();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        String resourceURL = DLMS_API_SERVER + "rest_api/api/hr/getIdsFromHrByDate/" + s_date + "/" + plant_location;
        HttpEntity<String> entity = new HttpEntity<String>(headers);
        ResponseEntity<String> response = restTemplate.exchange(resourceURL, HttpMethod.GET, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode;
            rootNode = objectMapper.readTree(response.getBody());
            JsonNode dataNode = rootNode.path("data");
            Iterator<JsonNode> elements = dataNode.elements();
            while (elements.hasNext()) {
                JsonNode idNode = elements.next();
                String id = idNode.get("id").textValue();
                employeeIds.add(id);
            }
        }
        System.out.println(employeeIds);

        return employeeIds;
    }

    public List<String> getEmployeeIdsFromDlms(String s_date, String plant_location) throws Exception{
        List<String> employeeIds = new ArrayList<>();

        String employees_sql = "select dl_id from dlms_drot_dl_allocation where date(shift_start)=? and plant_location=?";
        Object[] params = {s_date,plant_location};
        SqlRowSet result = jdbcPrimaryTemplate.queryForRowSet(employees_sql,params);

        while (result.next()){
            employeeIds.add(result.getString("dl_id"));
        }
        System.out.println(employeeIds);

        return employeeIds;
    }

    public HashMap getDlWorkHoursFromHr(String id, String s_date, String plant) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();

        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        String contentType = "application/json;charset=UTF-8";
        MediaType mediaType = new MediaType("application", "json", Charset.forName("UTF-8"));
        headers.setContentType(mediaType);
        String resourceURL = DLMS_API_SERVER + "rest_api/api/hr/getTotalHoursByEmployee/" + id + "/" + s_date;

        HttpEntity<String> entity = new HttpEntity<String>(headers);

        ResponseEntity<String> response = restTemplate.exchange(resourceURL, HttpMethod.GET, entity, String.class);
        HashMap dlWorkHour = null;

        if (null != response || response.getStatusCode() == HttpStatus.OK) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode;
            rootNode = objectMapper.readTree(response.getBody());

            JsonNode dataNode = rootNode.path("data");
            Iterator<JsonNode> elements = dataNode.elements();

            while (elements.hasNext()) {
                JsonNode idNode = elements.next();
                dlWorkHour = new HashMap();
                dlWorkHour.put("date", idNode.get("date").textValue());
                dlWorkHour.put("dl_name", idNode.get("name").textValue());
                dlWorkHour.put("hours", idNode.get("t_hours").asDouble());
                dlWorkHour.put("location", idNode.get("location").textValue());
                dlWorkHour.put("dl_id", idNode.get("id").textValue());
                dlWorkHour.put("dept", idNode.get("dept").textValue());

                HashMap leader = getEmployeeLeader(s_date,idNode.get("id").textValue(),plant);

                if (leader != null){
                    dlWorkHour.put("tl_id", leader.get("tl_id"));
                    dlWorkHour.put("tl_name", leader.get("tl_name"));
                    dlWorkHour.put("shift", leader.get("shift"));
                }else {
                    return null;
                }

                String plant_location = idNode.get("location").textValue();
                dlWorkHour.put("plant_location",plant_location);
                String work_center = getEmployeeWorkcenter(s_date, id, plant);
                dlWorkHour.put("work_center", work_center);
                String cost_center = Utility.getCostcenterByWorkcenter(PROLINK_API_SERVER,work_center,plant_location);
                dlWorkHour.put("cost_center", cost_center);
            }
           // System.out.println(dlWorkHour);
        }
        return dlWorkHour;
    }

    public String getEmployeeWorkcenter(String s_date, String id, String plant){
        String sql = "select orig_assigned_work_center from dlms_drot_dl_allocation " +
                "where dl_id=? and date(shift_start)=? and plant_location=?";
        Object[] params = new Object[]{ id, s_date, plant};

        SqlRowSet result = jdbcPrimaryTemplate.queryForRowSet(sql,params);

        while (result.next()){
            return result.getString("orig_assigned_work_center");
        }
        return "";

    }

    public HashMap getEmployeeLeader(String s_date, String id, String plant){
        HashMap leader = null;
        String sql = "select shift_type, assigned_to_tl_id, assigned_to_tl_name from dlms_drot_dl_allocation " +
                "where dl_id=? and date(shift_start)=? and plant_location=?";
        Object[] params = new Object[]{ id, s_date, plant};

        SqlRowSet result = jdbcPrimaryTemplate.queryForRowSet(sql,params);

        while (result.next()){
            leader = new HashMap();
            leader.put("tl_id", result.getString("assigned_to_tl_id"));
            leader.put("tl_name", result.getString("assigned_to_tl_name"));
            if (result.getString("shift_type").contains("DAY")){
                leader.put("shift", "day");
            }
            if (result.getString("shift_type").contains("NIGHT")){
                leader.put("shift", "night");
            }
            if (result.getString("shift_type").contains("MIDDLE")){
                leader.put("shift", "middle");
            }
        }

        return leader;
    }

    public void deleteExistDlmsHrWorkHour(String s_date, String plant_location) {
        String sql = "DELETE FROM dlms_hr_work_hour WHERE date(w_date)=? AND plant_location=?";
        Object[] params = new Object[]{s_date, plant_location};
        //int[] types = new int[] { Types.VARCHAR, Types.VARCHAR, Types.DATE,Types.VARCHAR, Types.TIMESTAMP };

        jdbcPrimaryTemplate.update(sql, params);
        System.out.println("=========== end of deleteExistDlmsHrWorkHour ==========");
    }



    public void insertDlmsHrWorkHour(HashMap dlWorkHour) {

        if (dlWorkHour == null){
            return;
        }

        String sql = "insert into dlms_hr_work_hour(dl_id, dl_name,w_date,hours,plant_location,dept" +
                ",tl_id,tl_name,work_center,shift,cost_center) values(?,?,?,?,?,?,?,?,?,?,?)";
        String shift = "";


        Object[] params = new Object[]{dlWorkHour.get("dl_id"), dlWorkHour.get("dl_name"),dlWorkHour.get("date"),
                dlWorkHour.get("hours"), dlWorkHour.get("plant_location"), dlWorkHour.get("dept"),
                dlWorkHour.get("tl_id"), dlWorkHour.get("tl_name"),
                dlWorkHour.get("work_center"),dlWorkHour.get("shift"),dlWorkHour.get("cost_center")};
        //System.out.println( Arrays.asList(params));
        jdbcPrimaryTemplate.update(sql, params);
    }

    public Map getHrLaborMinsByWorkCenter(String s_date, String work_center, String plant_location, String shift_type) {

        Map<String, String> out = null;
        List<Map<String, Object>> result = null;

        String sql = "select sum(hours)*60 total from dlms_hr_work_hour " +
                "where plant_location=? and date(w_date)=? and work_center=? and shift=?";

        result = jdbcPrimaryTemplate.queryForList(sql,
                new Object[]{plant_location, s_date, work_center, shift_type}
        );

/*        if (shift_type.equals("DAY")){
            sql = sql + "and day_tl_id is not null and day_tl_id<>'' ";
            result = jdbcPrimaryTemplate.queryForList(sql,
                    new Object[]{plant_location, s_date, work_center}
            );
        }

        if (shift_type.equals("NIGHT")){
            sql = sql + "and night_tl_id is not null and night_tl_id<>'' ";
            result = jdbcPrimaryTemplate.queryForList(sql,
                    new Object[]{plant_location, s_date, work_center}
            );
        }*/

        if (result != null){
            if (result.size() > 0) {
                out = new HashMap();
                for (Map item : result ) {
                    if (item.get("total") == null){
                        out.put("total", "0");
                    }else{
                        out.put("total", item.get("total").toString());
                    }
                }
            }
        } else {
            out.put("total", "0");
        }
        System.out.println("------------------------------------------end-----getHrLaborMinsByWorkCenter----");
        return out;
    }


}
