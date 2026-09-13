package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.vincent.common.exception.ServiceException;
import com.vincent.dto.RefundQueryDTO;
import com.vincent.dto.RefundStatusDTO;
import com.vincent.entity.RefundRecord;
import com.vincent.mapper.RefundRecordMapper;
import com.vincent.service.RefundRecordService;
import com.vincent.vo.PageVO;
import com.vincent.vo.RefundRecordVO;
import com.vincent.vo.RefundStatusVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundRecordServiceImpl extends ServiceImpl<RefundRecordMapper, RefundRecord> implements RefundRecordService {

    private final RefundRecordMapper refundRecordMapper;

    @Override
    public PageVO<RefundRecordVO> pageQuery(RefundQueryDTO dto) {
        Page<RefundRecord> page = new Page<>(dto.getPage(), dto.getPageSize());

        LocalDateTime startDateTime = dto.getStartDate() != null ? dto.getStartDate().atStartOfDay() : null;
        LocalDateTime endDateTime = dto.getEndDate() != null ? dto.getEndDate().plusDays(1).atStartOfDay() : null;

        LambdaQueryWrapper<RefundRecord> wrapper = new LambdaQueryWrapper<RefundRecord>()
                .eq(dto.getStatus() != null, RefundRecord::getStatus, dto.getStatus())
                .like(StringUtils.hasText(dto.getOrderNo()), RefundRecord::getRefundNo, dto.getOrderNo())
                .ge(dto.getStartDate() != null, RefundRecord::getCreatedAt, startDateTime)
                .le(dto.getEndDate() != null, RefundRecord::getCreatedAt, endDateTime)
                .orderByDesc(RefundRecord::getCreatedAt);

        Page<RefundRecord> result = page(page, wrapper);

        List<RefundRecordVO> voList = result.getRecords().stream().map(record -> {
            RefundRecordVO vo = new RefundRecordVO();
            BeanUtils.copyProperties(record, vo);
            return vo;
        }).collect(Collectors.toList());

        return new PageVO<>(result.getTotal(), dto.getPage(), dto.getPageSize(), voList);
    }

    @Override
    public RefundRecordVO getDetail(Long id) {
        RefundRecord record = getById(id);
        if (record == null) {
            throw new ServiceException("退款记录不存在");
        }
        RefundRecordVO vo = new RefundRecordVO();
        BeanUtils.copyProperties(record, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundStatusVO updateStatus(Long id, RefundStatusDTO dto) {
        RefundRecord record = getById(id);
        if (record == null) {
            throw new ServiceException("退款记录不存在");
        }

        int newStatus;
        String statusName;
        if ("approved".equals(dto.getStatus())) {
            newStatus = 1;
            statusName = "已通过";
        } else if ("rejected".equals(dto.getStatus())) {
            newStatus = 2;
            statusName = "已拒绝";
        } else {
            throw new ServiceException("无效的状态值");
        }

        record.setStatus(newStatus);
        record.setOperator("管理员");
        updateById(record);

        RefundStatusVO vo = new RefundStatusVO();
        vo.setId(id);
        vo.setStatus(newStatus);
        vo.setStatusName(statusName);
        vo.setReason(dto.getReason());
        return vo;
    }

}