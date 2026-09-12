package com.vincent.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.vincent.dto.BannerCreateDTO;
import com.vincent.entity.Banner;
import com.vincent.vo.BannerVO;

import java.util.List;

public interface BannerService extends IService<Banner> {

    List<BannerVO> listAll();

    void createBanner(BannerCreateDTO dto);

    void updateBanner(Long id, BannerCreateDTO dto);

    void deleteBanner(Long id);

    void toggleStatus(Long id, Integer status);

}