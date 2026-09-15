package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.vincent.common.exception.ServiceException;
import com.vincent.dto.SysConfigCreateDTO;
import com.vincent.entity.SysConfig;
import com.vincent.mapper.SysConfigMapper;
import com.vincent.service.SysConfigService;
import com.vincent.vo.SysConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    private final SysConfigMapper sysConfigMapper;

    @Override
    public List<SysConfigVO> listAll() {
        List<SysConfig> list = sysConfigMapper.selectList(
                new LambdaQueryWrapper<SysConfig>().orderByDesc(SysConfig::getCreatedAt)
        );
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public SysConfigVO getConfigDetail(Long id) {
        SysConfig config = getById(id);
        if (config == null) throw new ServiceException("\u914D\u7F6E\u9879\u4E0D\u5B58\u5728");
        return toVO(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createConfig(SysConfigCreateDTO dto) {
        SysConfig config = new SysConfig();
        BeanUtils.copyProperties(dto, config);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        save(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(Long id, SysConfigCreateDTO dto) {
        SysConfig config = getById(id);
        if (config == null) throw new ServiceException("\u914D\u7F6E\u9879\u4E0D\u5B58\u5728");
        BeanUtils.copyProperties(dto, config);
        config.setId(id);
        config.setUpdatedAt(LocalDateTime.now());
        updateById(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long id) {
        SysConfig config = getById(id);
        if (config == null) throw new ServiceException("\u914D\u7F6E\u9879\u4E0D\u5B58\u5728");
        removeById(id);
    }

    private SysConfigVO toVO(SysConfig config) {
        SysConfigVO vo = new SysConfigVO();
        BeanUtils.copyProperties(config, vo);
        return vo;
    }
}