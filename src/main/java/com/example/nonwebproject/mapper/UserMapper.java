package com.example.nonwebproject.mapper;

import com.example.nonwebproject.domain.UserDo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper {

    List<UserDo> selectAll();
}
