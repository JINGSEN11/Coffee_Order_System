#!/usr/bin/env python3
"""Create all missing backend files for Table QR management."""

import os

base = 'D:/Individual_Projects/Coffee_Order_System'

files = {}

# 1. Mapper
files[f'{base}/cos-server/src/main/java/com/vincent/mapper/TableQrMapper.java'] = """\
package com.vincent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vincent.entity.TableQr;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TableQrMapper extends BaseMapper<TableQr> {
}
"""

# 2. Service interface
files[f'{base}/cos-server/src/main/java/com/vincent/service/TableQrService.java'] = """\
package com.vincent.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.vincent.dto.TableQrCreateDTO;
import com.vincent.entity.TableQr;
import com.vincent.vo.PageVO;
import com.vincent.vo.TableQrVO;

import java.util.List;

public interface TableQrService extends IService<TableQr> {

    List<TableQrVO> listAll();

    PageVO<TableQrVO> pageQuery(String keyword, Integer page, Integer pageSize);

    TableQrVO getDetail(Long id);

    void createTable(TableQrCreateDTO dto);

    void updateTable(Long id, TableQrCreateDTO dto);

    void deleteTable(Long id);
}
"""

# 3. Service impl
files[f'{base}/cos-server/src/main/java/com/vincent/service/impl/TableQrServiceImpl.java'] = """\
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
"""

# 4. Controller
files[f'{base}/cos-server/src/main/java/com/vincent/controller/admin/TableQrController.java'] = """\
package com.vincent.controller.admin;

import com.vincent.common.Result;
import com.vincent.dto.TableQrCreateDTO;
import com.vincent.service.TableQrService;
import com.vincent.vo.PageVO;
import com.vincent.vo.TableQrVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("adminTableQrController")
@RequestMapping("/admin/table")
@RequiredArgsConstructor
@Slf4j
public class TableQrController {

    private final TableQrService tableQrService;

    @GetMapping("/list")
    public Result<PageVO<TableQrVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(tableQrService.pageQuery(keyword, page, pageSize));
    }

    @GetMapping("/all")
    public Result<List<TableQrVO>> all() {
        return Result.success(tableQrService.listAll());
    }

    @GetMapping("/{id}")
    public Result<TableQrVO> detail(@PathVariable Long id) {
        return Result.success(tableQrService.getDetail(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody TableQrCreateDTO dto) {
        tableQrService.createTable(dto);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody TableQrCreateDTO dto) {
        tableQrService.updateTable(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tableQrService.deleteTable(id);
        return Result.success();
    }
}
"""

for path, content in files.items():
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f'Created: {path}')

print('Done!')