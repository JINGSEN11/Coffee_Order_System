package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.vincent.common.exception.ServiceException;
import com.vincent.dto.TableQrCreateDTO;
import com.vincent.entity.TableQr;
import com.vincent.mapper.TableQrMapper;
import com.vincent.service.TableQrService;
import com.vincent.vo.PageVO;
import com.vincent.vo.TableQrVO;
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
public class TableQrServiceImpl extends ServiceImpl<TableQrMapper, TableQr> implements TableQrService {

    private final TableQrMapper tableQrMapper;

    @Override
    public List<TableQrVO> listAll() {
        List<TableQr> list = tableQrMapper.selectList(
                new LambdaQueryWrapper<TableQr>().orderByDesc(TableQr::getCreatedAt)
        );
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public PageVO<TableQrVO> pageQuery(String keyword, Integer pageNum, Integer pageSize) {
        Page<TableQr> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<TableQr> wrapper = new LambdaQueryWrapper<TableQr>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(TableQr::getTableNo, keyword)
                        .or()
                        .like(TableQr::getArea, keyword)
                )
                .orderByDesc(TableQr::getCreatedAt);

        Page<TableQr> result = page(page, wrapper);

        List<TableQrVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageVO<>(result.getTotal(), pageNum, pageSize, voList);
    }

    @Override
    public TableQrVO getDetail(Long id) {
        TableQr entity = getById(id);
        if (entity == null) throw new ServiceException("桌码不存在");
        return convertToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTable(TableQrCreateDTO dto) {
        TableQr entity = new TableQr();
        BeanUtils.copyProperties(dto, entity);
        entity.setShopId(1L);
        entity.setStatus(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        save(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTable(Long id, TableQrCreateDTO dto) {
        TableQr entity = getById(id);
        if (entity == null) throw new ServiceException("桌码不存在");
        BeanUtils.copyProperties(dto, entity);
        entity.setId(id);
        entity.setUpdatedAt(LocalDateTime.now());
        updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTable(Long id) {
        if (getById(id) == null) throw new ServiceException("桌码不存在");
        removeById(id);
    }

    private TableQrVO convertToVO(TableQr entity) {
        TableQrVO vo = new TableQrVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}