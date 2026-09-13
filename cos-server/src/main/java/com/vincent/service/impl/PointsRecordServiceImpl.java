package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.vincent.dto.PointsQueryDTO;
import com.vincent.entity.Member;
import com.vincent.entity.PointsRecord;
import com.vincent.mapper.MemberMapper;
import com.vincent.mapper.PointsRecordMapper;
import com.vincent.service.PointsRecordService;
import com.vincent.vo.PageVO;
import com.vincent.vo.PointsRecordVO;
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
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements PointsRecordService {

    private final PointsRecordMapper pointsRecordMapper;
    private final MemberMapper memberMapper;

    @Override
    public PageVO<PointsRecordVO> pageQuery(PointsQueryDTO dto) {
        Page<PointsRecord> page = new Page<>(dto.getPage(), dto.getPageSize());

        LocalDateTime startDateTime = dto.getStartDate() != null ? dto.getStartDate().atStartOfDay() : null;
        LocalDateTime endDateTime = dto.getEndDate() != null ? dto.getEndDate().plusDays(1).atStartOfDay() : null;

        LambdaQueryWrapper<PointsRecord> wrapper = new LambdaQueryWrapper<PointsRecord>()
                .eq(dto.getUserId() != null, PointsRecord::getUserId, dto.getUserId())
                .eq(dto.getType() != null, PointsRecord::getType, dto.getType())
                .ge(dto.getStartDate() != null, PointsRecord::getCreatedAt, startDateTime)
                .le(dto.getEndDate() != null, PointsRecord::getCreatedAt, endDateTime)
                .orderByDesc(PointsRecord::getCreatedAt);

        Page<PointsRecord> result = page(page, wrapper);

        List<PointsRecordVO> voList = result.getRecords().stream().map(record -> {
            PointsRecordVO vo = new PointsRecordVO();
            BeanUtils.copyProperties(record, vo);

            // 填充用户昵称
            Member member = memberMapper.selectById(record.getUserId());
            if (member != null) {
                vo.setUserNickname(member.getNickname());
            }

            return vo;
        }).collect(Collectors.toList());

        return new PageVO<>(result.getTotal(), dto.getPage(), dto.getPageSize(), voList);
    }

}