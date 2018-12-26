package com.example.nonwebproject.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;
import com.example.nonwebproject.utility.Utility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OffStandardService {

    @Qualifier("jdbcPrimaryTemplate")
    @Autowired
    JdbcTemplate jdbcPrimaryTemplate;

    @Autowired
    private HrService hrService;

    @Autowired
    private QadService qadService;

    @Value("${DLMS_API_SERVER}")
    private String DLMS_API_SERVER;

    @Value("${PROLINK_API_SERVER}")
    private String PROLINK_API_SERVER;

    public void runTestCommand() {
        String s_date = "2018-11-07";
        String plant_location = "CZ";
        String shift_tye = "day";
        try {
            callCreateOffStandardData(s_date, plant_location, shift_tye);
            callCreateOffStandardData(s_date, plant_location, "night");
        }catch (Exception e){
            System.out.println(e.fillInStackTrace());
        }
    }

    public void runCommand(String s_date, String plant_location) {

        String day_shift = "day";
        String night_shift = "night";
        try {
            callCreateOffStandardData(s_date, plant_location, day_shift);
            callCreateOffStandardData(s_date, plant_location, night_shift);
        }catch (Exception e){
            System.out.println(e.fillInStackTrace());
        }
    }

    public void deleteExistDlmsWorkcenterOffstandard(String plant_location, String shift_type, String s_date, String work_center) {
        String sql = "DELETE FROM dlms_workcenter_offstandard " +
                "WHERE DATE(w_date)=? AND plant_location=? AND shift_type=? AND work_center=?";
        Object[] params = new Object[]{s_date, plant_location, shift_type, work_center};

        jdbcPrimaryTemplate.update(sql, params);
        System.out.println("================= end of ExistDlmsWorkcenterOffstandard ====");
    }

    public Map getRealWorkTimeByWorkcenter2(String s_date, String shift_type, String work_center, String plant_location) {
        System.out.println("----------------start-----getRealWorkTimeByWorkcenter------------------------------");
        Map out = new HashMap();

        Map shift_data = Utility.generateShiftDate(s_date, shift_type);
        String shift_start = (String) shift_data.get("shift_start");
        String shift_end = (String) shift_data.get("shift_end");

        String total_loan_in="0";
        String total_loan_out="0";

        String sql_loan_in = "select case when work_start is not null and work_end is not null" +
                " then sum(ROUND(time_to_sec((TIMEDIFF(work_end, work_start))) / 60)) " +
                " else 0 end total_loan_in " +
                " FROM dlms_drot_dl_history " +
                " WHERE plant_location=? and shift_start=? " +
                " AND work_center=? and dl_id not in (" +
                " select dl_id from dlms_drot_dl_allocation where  shift_start=? and orig_assigned_work_center=? )";

        SqlRowSet loan_in_result= jdbcPrimaryTemplate.queryForRowSet(sql_loan_in,
                new Object[]{plant_location,shift_start, work_center,shift_start, work_center});

        while (loan_in_result.next()){
            total_loan_in = loan_in_result.getString("total_loan_in");
        }

        String sql_loan_out = " select case when work_start is not null and work_end is not null " +
                " then sum(ROUND(time_to_sec((TIMEDIFF(work_end, work_start))) / 60)) " +
                " else 0 end total_loan_out " +
                " FROM dlms_drot_dl_history " +
                " WHERE plant_location=? and shift_start=? " +
                "  and work_center<>? AND dl_id in (" +
                " select dl_id from dlms_drot_dl_allocation where  shift_start=? and orig_assigned_work_center=? ) ";

        SqlRowSet loan_out_result= jdbcPrimaryTemplate.queryForRowSet(sql_loan_out,
                new Object[]{plant_location,shift_start, work_center,shift_start, work_center});

        while (loan_out_result.next()) {
            total_loan_out = loan_out_result.getString("total_loan_out");
        }

        out.put("loan_in_mins", total_loan_in);
        out.put("loan_out_mins", total_loan_out);

        return out;
    }

    public void callCreateOffStandardData(String s_date, String plant_location, String shift_type) throws Exception {
        List<HashMap<String, String>> work_centers = getWorkCenterByDate(s_date, shift_type,plant_location);

        if (null == work_centers || work_centers.size() == 0) {
            return;
        }
        Utility.deleteFile(s_date);
        qadService.setFileName(s_date);

        for (int i = 0; i < work_centers.size(); i++) {

            Map loan_data = getRealWorkTimeByWorkcenter2(s_date, shift_type, work_centers.get(i).get("work_center"), plant_location);
            Map production = qadService.calculateOutputMins(s_date, work_centers.get(i).get("work_center"), plant_location, shift_type.toUpperCase());
            Map hr_minutes = hrService.getHrLaborMinsByWorkCenter(s_date, work_centers.get(i).get("work_center"), plant_location, shift_type.toUpperCase());

            insertDlmsWorkcenterOffstandard(s_date, work_centers.get(i).get("work_center"),
                    work_centers.get(i).get("cost_center"), shift_type, plant_location,
                    loan_data, production, hr_minutes);
        }
    }

/*    public List<HashMap<String, String>> getWorkcenter(String s_date, String shift_type) {
        List out = new ArrayList<HashMap<String, String>>();

        String sql = "SELECT DISTINCT(op_line) work_center,cost_center FROM dlms_qad_production_info" +
                " WHERE shift=? AND DATE(op_date)=?";

        Object[] params = new Object[]{shift_type, s_date};
        List<Map<String, Object>> result = jdbcPrimaryTemplate.queryForList(sql, params);

        if (result.size() > 0) {
            for (Map item : result) {
                HashMap<String, String> one = new HashMap<>();
                one.put("work_center", (String) item.get("work_center"));
                one.put("cost_center", (String) item.get("cost_center"));

                out.add(one);
            }
        }
        return out;
    }*/

    public void insertDlmsWorkcenterOffstandard(String s_date, String work_center, String cost_center,
                                                String shift_type, String plant_location,
                                                Map loan_data, Map production, Map hr_minutes) {

        System.out.println("----------------start-----insertDlmsWorkcenterOffstandard------------------------------");
        deleteExistDlmsWorkcenterOffstandard(plant_location, shift_type, s_date, work_center);

        String sql = "insert into dlms_workcenter_offstandard(" +
                "w_date, shift_type, work_center,cost_center, hr_work_minutes, team_work_minutes, " +
                "team_loan_in_minutes, team_loan_out_minutes, team_real_work_minutes " +
                ",production_num,production_minutes," +
                "diff_hr_and_real, " +
                "off_standard, " +
                "off_heads, " +
                "plant_location, " +
                "vtyc_pns, production_nums, labor_minutes) " +
                "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?" +
                ",?,?,?,?" +
                ")";

        Double total_loan_out_minutes = 0.0;
        Double total_loan_in_minutes = 0.0;
        int team_real_work_minutes = 0;
        int team_work_minutes = 0;
        int team_loan_in_minutes = 0;
        double diff_hr_and_real = 0.0;
        double off_standard = 0.0;
        double off_heads = 0.0;
        int production_total_production = 0;
        double production_total_production_mins = 0.0;
        String production_vtyc_pns = "";
        String production_production_nums = "";
        String production_labor_minutes = "";
        int hr_minutes_total = 0;

        total_loan_out_minutes = Double.valueOf((String)loan_data.get("loan_out_mins"));
        total_loan_in_minutes = Double.valueOf((String)loan_data.get("loan_in_mins"));

        if (hr_minutes != null){
            hr_minutes_total = Integer.parseInt((String)hr_minutes.get("total")) ;
        }

        if (hr_minutes.get("total") != null && hr_minutes.get("total") != "") {
            team_real_work_minutes = hr_minutes_total - total_loan_out_minutes.intValue() + total_loan_in_minutes.intValue();
        }else{
            team_real_work_minutes = total_loan_in_minutes.intValue() - total_loan_out_minutes.intValue();
        }

        if (production != null) {
            diff_hr_and_real = (double) production.get("total_production_mins")
                    - team_real_work_minutes;

            if ((double) production.get("total_production_mins") != 0.0) {
                off_standard = diff_hr_and_real / (double) production.get("total_production_mins");
            }

            production_total_production = (int) production.get("total_production");
            production_total_production_mins = (double) production.get("total_production_mins");
            production_vtyc_pns = (String) production.get("vtyc_pns");
            production_production_nums = (String) production.get("production_nums");
            production_labor_minutes = (String) production.get("labor_minutes");
        }else{
            diff_hr_and_real = 0 - team_real_work_minutes;
        }

        off_heads = diff_hr_and_real / 480;

        Object[] params = new Object[]{
                s_date, shift_type, work_center, cost_center, hr_minutes_total, team_work_minutes,
                total_loan_in_minutes, total_loan_out_minutes.intValue(), team_real_work_minutes,
                production_total_production, production_total_production_mins,
                diff_hr_and_real,
                off_standard,
                off_heads,
                plant_location,
                production_vtyc_pns, production_production_nums, production_labor_minutes
        };

        jdbcPrimaryTemplate.update(sql, params);
        System.out.println("------------------------------------------End-----insertDlmsWorkcenterOffstandard----");
    }


    private List<HashMap<String, String>>  getWorkCenterByDate(String s_date, String shift_type, String plant_location) throws Exception{
        List out = new ArrayList<HashMap<String, String>>();

        Map shift_data = Utility.generateShiftDate(s_date, shift_type);
        String shift_start = (String) shift_data.get("shift_start");
        String shift_end = (String) shift_data.get("shift_end");

        String sql = "select distinct(work_center) work_center from dlms_drot_dl_history " +
                "where shift_start=? and plant_location=? " +
                "union " +
                "select distinct(op_line) work_center from dlms_qad_production_info " +
                "where op_date=? and shift=? and plant_location=? " +
                "order by work_center";

        Object[] params = new Object[]{
                shift_start,plant_location, s_date, shift_type,plant_location
        };
        List<Map<String, Object>> result = jdbcPrimaryTemplate.queryForList(sql, params);

        if (result.size() > 0) {
            for (Map item : result) {
                HashMap<String, String> one = new HashMap<>();
                one.put("work_center", (String) item.get("work_center"));
                String cost_center = Utility.getCostcenterByWorkcenter(PROLINK_API_SERVER,(String)item.get("work_center"),plant_location);
                one.put("cost_center", cost_center);
                out.add(one);
            }
        }
        return out;
    }

}
