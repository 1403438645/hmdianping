package com.athdu.travel.dianpingproject.dto;

import lombok.Data;

import java.util.List;

/**
 * @author baizhejun
 * @create 2022 -10 -24 - 21:00
 */
@Data
public class ScrollResult {
    private List<?> list;
    private Long minTime;
    private Integer offset;
}
