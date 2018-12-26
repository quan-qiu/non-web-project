package com.example.nonwebproject.service;

import com.example.nonwebproject.domain.UserDo;
import com.example.nonwebproject.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.List;

@Service
@EnableTransactionManagement
public class UserService {

    @Autowired
    private UserMapper userMapper;

    public List<UserDo> getUsers(){
        return userMapper.selectAll();
    }

    public String getHello(){
        return "hello !!!!";
    }
}
