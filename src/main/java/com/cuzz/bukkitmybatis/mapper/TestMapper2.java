package com.cuzz.bukkitmybatis.mapper;

import com.cuzz.bukkitmybatis.model.Group;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TestMapper2 {

    Group getGroupByName(String name);

    Group getGroupById(Long id);

    List<Group> listGroups(@Param("offset") int offset, @Param("limit") int limit);

    int insertGroup(Group group);

    int updateGroup(Group group);

    int deleteGroup(Long id);
}
