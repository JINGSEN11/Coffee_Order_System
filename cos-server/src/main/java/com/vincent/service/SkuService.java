package com.vincent.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.vincent.dto.SkuCreateDTO;
import com.vincent.dto.SkuQueryDTO;
import com.vincent.entity.Sku;
import com.vincent.vo.PageVO;
import com.vincent.vo.SkuVO;

public interface SkuService extends IService<Sku> {

    PageVO<SkuVO> pageQuery(SkuQueryDTO dto);

    PageVO<SkuVO> warnPage(Integer page, Integer pageSize);

    SkuVO getSkuDetail(Long id);

    void updateSku(Long id, SkuCreateDTO dto);

}