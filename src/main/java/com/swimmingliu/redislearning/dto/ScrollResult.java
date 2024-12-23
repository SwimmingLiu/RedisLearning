package com.swimmingliu.redislearning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ScrollResult {
    private List<?> list;
    private Long minTime;
    private Integer offset;
}
