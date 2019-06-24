package com.example.nonwebproject.service;

import com.example.nonwebproject.utility.Utility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DlmsService {

    @Qualifier("jdbcPrimaryTemplate")
    @Autowired
    JdbcTemplate jdbcPrimaryTemplate;


    public void operation() throws Exception{

        List<Map<String,Object>> dlms_workcenter_offstandard_ids = getDlmsWorkcenterOffstandardId();

        if (dlms_workcenter_offstandard_ids != null && dlms_workcenter_offstandard_ids.size()>0){
            for (Map item : dlms_workcenter_offstandard_ids) {
                List<Map<String, Object>> info =  getDlmsOffstandardInfoById((Integer) item.get("dlms_workcenter_offstandard_id"));

                /*if (info != null && info.size()>0){
                    for (Map one_info : info){
                        Date v_w_date = (Date) one_info.get("w_date");
                        String v_shift_type = (String) one_info.get("shift_type");
                        String v_work_center = (String) one_info.get("work_center");
                        String v_memo = (String) one_info.get("memo");
                        int v_loan_to_undirect_department = (Integer) one_info.get("loan_to_undirect_department");
                        int v_try_new_product = (Integer) one_info.get("try_new_product");
                        int v_produce_new_product = (Integer) one_info.get("produce_new_product");
                        int v_unplan_stop = (Integer) one_info.get("unplan_stop");
                        int v_new_employee = (Integer) one_info.get("new_employee");
                        int v_labor_minute_too_less = (Integer) one_info.get("labor_minute_too_less");
                        int v_hr_data_is_wrong = (Integer) one_info.get("hr_data_is_wrong");

                        int wc_status_id = 0;

                        List<Map<String,Object>> wc_status = getWorkCenterId(v_w_date, v_work_center,v_shift_type);

                        if (wc_status != null && wc_status.size()>0){
                            for (Map one: wc_status){
                                wc_status_id = (Integer) one.get("dlms_drot_wc_status_id");
                            }

                            insertDlmsOffstandardMemo(wc_status_id, v_memo, v_loan_to_undirect_department, v_try_new_product, v_produce_new_product,
                                    v_unplan_stop, v_new_employee, v_labor_minute_too_less,v_hr_data_is_wrong);
                        }
                    }
                }*/
                if (info != null && info.size()>0) {
                    int dlms_workcenter_offstandard_id = (Integer) item.get("dlms_workcenter_offstandard_id");
                    System.out.println(dlms_workcenter_offstandard_id);
                    for (Map one_info : info) {
                        Date v_w_date = (Date) one_info.get("w_date");
                        String v_shift_type = (String) one_info.get("shift_type");
                        String v_work_center = (String) one_info.get("work_center");
                        int wc_status_id = 0;
                        List<Map<String, Object>> wc_status = getWorkCenterId(v_w_date, v_work_center, v_shift_type);

                        if (wc_status != null && wc_status.size() > 0) {
                            for (Map one : wc_status) {
                                wc_status_id = (Integer) one.get("dlms_drot_wc_status_id");
                            }

                            insertDlmsOffstandard(dlms_workcenter_offstandard_id, wc_status_id);
                        }
                    }
                }
            }
        }

    }

    private void insertDlmsOffstandard(int dlms_workcenter_offstandard_id, int wc_status_id) throws Exception{
        String insert_sql = "update dlms_workcenter_offstandard set dlms_drot_wc_status_id=? where dlms_workcenter_offstandard_id=?";


        Object[] insert_params = new Object[]{
                wc_status_id, dlms_workcenter_offstandard_id
        };

        jdbcPrimaryTemplate.update(insert_sql, insert_params);
    }

    private void insertDlmsOffstandardMemo(int wc_status_id, String v_memo, int v_loan_to_undirect_department, int v_try_new_product, int v_produce_new_product,
                                           int v_unplan_stop, int v_new_employee, int v_labor_minute_too_less,int v_hr_data_is_wrong
                                           ) throws Exception{
        String insert_sql = "insert into dlms_workcenter_offstandard_memo( " +
                "dlms_drot_wc_status_id, memo, loan_to_undirect_department, try_new_product, produce_new_product, " +
                "unplan_stop, new_employee, labor_minute_too_less, hr_data_is_wrong) " +
                "values(?,?,?,?,?,?,?,?,?)";


        Object[] insert_params = new Object[]{
                wc_status_id, v_memo, v_loan_to_undirect_department, v_try_new_product, v_produce_new_product,
                v_unplan_stop, v_new_employee, v_labor_minute_too_less,v_hr_data_is_wrong
        };

        jdbcPrimaryTemplate.update(insert_sql, insert_params);
    }

    private List<Map<String, Object>> getDlmsOffstandardInfoById(Integer id) throws Exception{
        String sql = " select  * from dlms_workcenter_offstandard where dlms_workcenter_offstandard_id=?" ;
        Object[] params = new Object[]{
                id
        };
        List<Map<String, Object>> result = jdbcPrimaryTemplate.queryForList(sql,params);
        if (result.size() > 0) {
            return result;
        }else{
            return null;
        }
    }

    private List<Map<String,Object>> getDlmsWorkcenterOffstandardId() throws Exception{
        String sql = " select dlms_workcenter_offstandard_id from dlms_workcenter_offstandard where dlms_workcenter_offstandard_id>='43086'" ;
        List<Map<String, Object>> result = jdbcPrimaryTemplate.queryForList(sql);
        if (result.size() > 0) {
            return result;
        }else{
            return null;
        }

    }

    private List<Map<String, Object>> getWorkCenterId(Date s_date, String work_center,String shift_type) throws Exception{

        String sql = " select * from dlms_drot_wc_status" +
                " where date(shift_start)=? and shift_type like ? and work_center=?;" ;

        Object[] params = new Object[]{
                s_date,"%" + shift_type + "%", work_center
        };
        List<Map<String, Object>> result = jdbcPrimaryTemplate.queryForList(sql, params);

        if (result.size() > 0) {
            return result;
        }else{
            return null;
        }
    }

}
