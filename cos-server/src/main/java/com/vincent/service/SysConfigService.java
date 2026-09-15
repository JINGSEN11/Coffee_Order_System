package com.vincent.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.vincent.dto.SysConfigCreateDTO;
import com.vincent.entity.SysConfig;
import com.vincent.vo.SysConfigVO;

import java.util.List;

public interface SysConfigService extends IService<SysConfig> {

    List<SysConfigVO> listAll();

    SysConfigVO getConfigDetail(Long id);

    void createConfig(SysConfigCreateDTO dto);

    void updateConfig(Long id, SysConfigCreateDTO dto);

    void deleteConfig(Long id);
}