package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.vincent.dto.OpLogQueryDTO;
import com.vincent.entity.OpLog;
import com.vincent.mapper.OpLogMapper;
import com.vincent.service.OpLogService;
import com.vincent.vo.OpLogVO;
import com.vincent.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpLogServiceImpl extends ServiceImpl<OpLogMapper, OpLog> implements OpLogService {

    private final OpLogMapper opLogMapper;

    @Override
    public PageVO<OpLogVO> pageQuery(OpLogQueryDTO dto) {
        Page<OpLog> page = new Page<>(dto.getPage(), dto.getPageSize());

        LocalDateTime startDateTime = dto.getStartDate() != null ? dto.getStartDate().atStartOfDay() : null;
        LocalDateTime endDateTime = dto.getEndDate() != null ? dto.getEndDate().plusDays(1).atStartOfDay() : null;

        LambdaQueryWrapper<OpLog> wrapper = new LambdaQueryWrapper<OpLog>()
                .like(dto.getOperator() != null && !dto.getOperator().isEmpty(), OpLog::getOperator, dto.getOperator())
                .like(dto.getModule() != null && !dto.getModule().isEmpty(), OpLog::getModule, dto.getModule())
                .ge(dto.getStartDate() != null, OpLog::getCreatedAt, startDateTime)
                .le(dto.getEndDate() != null, OpLog::getCreatedAt, endDateTime)
                .orderByDesc(OpLog::getCreatedAt);

        Page<OpLog> result = page(page, wrapper);

        List<OpLogVO> voList = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageVO<>(result.getTotal(), dto.getPage(), dto.getPageSize(), voList);
    }

    private OpLogVO toVO(OpLog opLog) {
        OpLogVO vo = new OpLogVO();
        BeanUtils.copyProperties(opLog, vo);
        return vo;
    }
}