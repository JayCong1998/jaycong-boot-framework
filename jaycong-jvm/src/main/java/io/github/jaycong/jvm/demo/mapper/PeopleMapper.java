package io.github.jaycong.jvm.demo.mapper;

import io.github.jaycong.jvm.demo.bean.People;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <pre>
 *    @author  : shkstart
 *    email   : shkstart@126.com
 *    time    : 15:21
 *    version : v1.0
 * </pre>
 */
@Mapper
public interface PeopleMapper {
    List<People> getPeopleList();
}
