package com.example.nonwebproject.service;

import com.example.nonwebproject.utility.Utility;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.*;

@Service
public class QadService {
    @Qualifier("jdbcPrimaryTemplate")
    @Autowired
    JdbcTemplate jdbcPrimaryTemplate;

    @Value("${DLMS_API_SERVER}")
    private String DLMS_API_SERVER;

    @Value("${PROLINK_API_SERVER}")
    private String PROLINK_API_SERVER;

    protected String file_name;

    public void setFileName(String file_name){
        this.file_name = file_name;
    }
    public String getFileName(){
        return this.file_name;
    }

    public void runTestCommand() {
        String s_date = "2019-03-18";
        String plant_location = "CZ";
        String shift_type = "day";

        try {
            deleteExistQadProductionInfo(s_date, plant_location);
            callQadApiDay(s_date,plant_location);
            //callQadApiDay(s_date,plant_location,"night");
           // secondToDetaTime(String.valueOf(3800));
        }catch (Exception e){
            System.out.println(e.fillInStackTrace());
        }
    }

    public void runCommand(String s_date, String plant_location) {
        try {
            deleteExistQadProductionInfo(s_date, plant_location);
            callQadApiDay(s_date,plant_location);
        }catch (Exception e){
            System.out.println(e.fillInStackTrace());
        }
    }

    public void deleteExistQadProductionInfo(String s_date, String plant_location) {
        String sql = "DELETE FROM dlms_qad_production_info WHERE date(op_date)=? AND plant_location=?";
        Object[] params = new Object[]{s_date, plant_location};

        jdbcPrimaryTemplate.update(sql, params);
        System.out.println("=========== end of deleteExistDlmsHrWorkHour ==========");
    }

    public void callQadApiDay(String s_date, String plant_location) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();

        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        String contentType = "application/json;charset=UTF-8";
        MediaType mediaType = new MediaType("application", "json", Charset.forName("UTF-8"));
        headers.setContentType(mediaType);

        String router = "";

        router = "/productionByDate/";

        String resourceURL = PROLINK_API_SERVER + plant_location + router + s_date;

        System.out.println("------------------ resourceURL:" + resourceURL);
        HttpEntity<String> entity = new HttpEntity<String>(headers);

        ResponseEntity<String> response = restTemplate.exchange(resourceURL, HttpMethod.GET, entity, String.class);

        System.out.println(Arrays.asList(response));

        if (response.getStatusCode() == HttpStatus.OK) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode;

            rootNode = objectMapper.readTree(response.getBody());
            JsonNode dataNode = rootNode.path("data");
            Iterator<JsonNode> elements = dataNode.elements();
            while (elements.hasNext()) {
                HashMap qadData = new HashMap();
                JsonNode idNode = elements.next();
/*                System.out.println("---------------------------------------------------------------"+idNode.get("op_part").textValue());
                System.out.println(idNode.get("op_wo_op").textValue());
               // System.out.println(idNode.get("shift").textValue());
                System.out.println(idNode.get("op_emp").textValue());
                System.out.println(idNode.get("op_line").textValue());
                System.out.println(idNode.get("op_qty_comp").textValue());
                System.out.println(idNode.get("op_dept").textValue());
                System.out.println(idNode.get("op_date").textValue());*/

                qadData.put("op_part",idNode.get("op_part").textValue());
                qadData.put("op_wo_op",idNode.get("op_wo_op").textValue());
               // qadData.put("shift", idNode.get("shift").textValue());
                qadData.put("op_emp",  idNode.get("op_emp").textValue());
                qadData.put("op_line",  idNode.get("op_line").textValue());
                qadData.put("op_qty_comp",  idNode.get("op_qty_comp").textValue());
                qadData.put("cost_center",  idNode.get("op_dept").textValue());
                qadData.put("op_date", idNode.get("op_date").textValue().substring(0,10) + " " + secondToDetaTime(idNode.get("op_time").textValue()));

                /*qadData.entrySet()
                        .stream()
                        .forEach(System.out::println);*/
                insertDlmsQadProductionInfo(qadData, s_date, plant_location);
            }
        }
    }

    public void insertDlmsQadProductionInfo(HashMap qadData, String date, String plant_location) {

        String sql = "insert into dlms_qad_production_info(op_wo_op,op_emp," +
                    "op_line,production_num,op_part,op_date,plant_location,cost_center) " +
                    "values(?,?,?,?,?,?,?,?)";
        Object[] params = new Object[]{qadData.get("op_wo_op"), qadData.get("op_emp"),
                qadData.get("op_line"),(int)(Double.parseDouble((String)qadData.get("op_qty_comp"))), qadData.get("op_part"), qadData.get("op_date"), plant_location, qadData.get("cost_center")};

        jdbcPrimaryTemplate.update(sql, params);
        System.out.println("=========== end of insertDlmsQadProductionInfo ==========");
    }

    public Map calculateOutputMins(String s_date, String work_center, String plant_location, String shift_type) {
        System.out.println("----------------start-----calculateOutputMins------------------------------");
        Map out = null;

        List<Map<String, Object>> production = getProductionInfoByWorkCenter(s_date, work_center, plant_location, shift_type);

        if (production == null) {
            return null;
        }

        int total_production = 0;
        double total_production_mins = 0;
        if (production.size() > 0) {
            String[] s_vtyc_pn = new String[production.size()];
            String[] s_production_num = new String[production.size()];
            String[] s_labor_minutes = new String[production.size()];

            int i = 0;
            for (Map item : production) {
                String vtyc_pn = (String) item.get("op_part");
                int production_num = (int) item.get("production_num");
                Map labor_mins = getLaborMinsByVtycPn(vtyc_pn, s_date,plant_location);
                double labor_minutes = 0.0;
                if (labor_mins.get("labor_minutes") == null) {

                    List<String> no_labor_mins = new ArrayList<>();
                    no_labor_mins.add("vtyc_pn:" + vtyc_pn);
                    no_labor_mins.add(" production_num:" + production_num);
                    no_labor_mins.add(" labor_minutes:" + labor_mins.get("labor_minutes"));
                    no_labor_mins.add("");
                    Utility.writeToFile(getFileName(), no_labor_mins);
                    labor_minutes = 0.0;
                } else {
                    labor_minutes = (double) labor_mins.get("labor_minutes");
                }

                double production_mins = production_num * (labor_minutes);

                total_production += production_num;
                total_production_mins += production_mins;
                s_vtyc_pn[i] = vtyc_pn;
                s_production_num[i] = Integer.toString(production_num);
                s_labor_minutes[i] = Double.toString(labor_minutes);

                i++;
            }
            out = new HashMap();
            out.put("vtyc_pns", Utility.mytoString(s_vtyc_pn, ", "));
            out.put("production_nums", Utility.mytoString(s_production_num, ", "));
            out.put("labor_minutes", Utility.mytoString(s_labor_minutes, ", "));
            out.put("total_production", total_production);
            out.put("total_production_mins", total_production_mins);
        }
        System.out.println("------------------------------------------end-----calculateOutputMins----");
        return out;
    }

    public Map calculateOutputMins2(String s_date,String shift_start, String shift_finish, String work_center, String plant_location) {
        System.out.println("----------------start-----calculateOutputMins------------------------------");
        Map out = null;

        List<Map<String, Object>> production = getProductionInfoByWorkCenter2(shift_start,shift_finish, work_center, plant_location);

        System.out.println(Arrays.asList(production));

        if (production == null) {
            return null;
        }

        int total_production = 0;
        double total_production_mins = 0;
        if (production.size() > 0) {
            String[] s_vtyc_pn = new String[production.size()];
            String[] s_production_num = new String[production.size()];
            String[] s_labor_minutes = new String[production.size()];

            int i = 0;
            for (Map item : production) {
                if (item.get("op_part")==null){
                    return null;
                }
                String vtyc_pn = (String) item.get("op_part");
                int production_num = (int)(((BigDecimal)item.get("production_num")).doubleValue());
                Map labor_mins = getLaborMinsByVtycPn(vtyc_pn, s_date,plant_location);
                double labor_minutes = 0.0;
                if (labor_mins.get("labor_minutes") == null) {

                    List<String> no_labor_mins = new ArrayList<>();
                    no_labor_mins.add("vtyc_pn:" + vtyc_pn);
                    no_labor_mins.add(" production_num:" + production_num);
                    no_labor_mins.add(" labor_minutes:" + labor_mins.get("labor_minutes"));
                    no_labor_mins.add("");
                    Utility.writeToFile(getFileName(), no_labor_mins);
                    labor_minutes = 0.0;
                } else {
                    labor_minutes = (double) labor_mins.get("labor_minutes");
                }

                double production_mins = production_num * (labor_minutes);

                total_production += production_num;
                total_production_mins += production_mins;
                s_vtyc_pn[i] = vtyc_pn;
                s_production_num[i] = String.valueOf(production_num);
                s_labor_minutes[i] = Double.toString(labor_minutes);

                i++;
            }
            out = new HashMap();
            out.put("vtyc_pns", Utility.mytoString(s_vtyc_pn, ", "));
            out.put("production_nums", Utility.mytoString(s_production_num, ", "));
            out.put("labor_minutes", Utility.mytoString(s_labor_minutes, ", "));
            out.put("total_production", total_production);
            out.put("total_production_mins", total_production_mins);
        }
        System.out.println("------------------------------------------end-----calculateOutputMins----");
        return out;
    }

    public Map getLaborMinsByVtycPn(String vtyc_pn, String s_date, String plant_location) {
        Map out = new HashMap();
        String sql = "select vtyc_pn,cws_labor_minutes from dlms_current_working_standard" +
                " where vtyc_pn=? and date(expired_date)>=? and plant_location=? limit 1";

        List<Map<String, Object>> result = jdbcPrimaryTemplate.queryForList(sql,
                new Object[]{vtyc_pn, s_date,plant_location}
        );

        if (result.size() > 0) {
            for (Map item : result
                    ) {
                out.put("vtyc_pn", item.get("vtyc_pn"));
                out.put("labor_minutes", item.get("cws_labor_minutes"));
            }
        }
        return out;
    }

    public List getProductionInfoByWorkCenter(String s_date, String work_center, String plant_location, String shift_type) {

        String sql = "SELECT * FROM dlms_qad_production_info" +
                " where date(op_date)=? and upper(op_line)=upper(?) and plant_location=? and upper(shift)=?";

        List<Map<String, Object>> result = jdbcPrimaryTemplate.queryForList(sql,
                new Object[]{s_date, work_center, plant_location, shift_type}
        );

        if (result.size() > 0) {
            return result;
        } else {
            return null;
        }

    }

    public List getProductionInfoByWorkCenter2(String shift_start,String shift_finish, String work_center, String plant_location) {

        String sql = "SELECT sum(production_num) production_num, op_part FROM dlms_qad_production_info" +
                " where op_date>=?  and op_date<=? and upper(op_line)=upper(?) and plant_location=?";

        Object[] params = {shift_start,shift_finish, work_center, plant_location};

        List<Map<String, Object>> result = jdbcPrimaryTemplate.queryForList(sql,
               params
        );

        if (null == result) {
            return null;
        } else {
            return result;
        }

    }

    public String secondToDetaTime(String seconds){
        int int_seconds = Integer.valueOf(seconds);

        int op_hours = (int)Math.floor(int_seconds/3600);
       // System.out.println("hours: " + (int)op_hours);
        int op_minutes = (int)Math.floor((int_seconds-(op_hours * 3600))/60);
      //  System.out.println("minutes: " + (int)op_minutes);
        int op_seconds = int_seconds - (op_hours * 3600) - (op_minutes * 60);

        return String.format("%02d",op_hours)+ ":" + String.format("%02d",op_minutes) + ":" + String.format("%02d", op_seconds);

    }
}
