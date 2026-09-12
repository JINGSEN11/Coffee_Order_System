package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.vincent.common.exception.ServiceException;
import com.vincent.dto.BannerCreateDTO;
import com.vincent.entity.Banner;
import com.vincent.mapper.BannerMapper;
import com.vincent.service.BannerService;
import com.vincent.vo.BannerVO;
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
public class BannerServiceImpl extends ServiceImpl<BannerMapper, Banner> implements BannerService {

    private final BannerMapper bannerMapper;

    @Override
    public List<BannerVO> listAll() {
        List<Banner> list = bannerMapper.selectList(
                new LambdaQueryWrapper<Banner>()
                        .orderByAsc(Banner::getSort)
        );

        return list.stream().map(banner -> {
            BannerVO vo = new BannerVO();
            BeanUtils.copyProperties(banner, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createBanner(BannerCreateDTO dto) {
        Banner banner = new Banner();
        BeanUtils.copyProperties(dto, banner);
        banner.setCreatedAt(LocalDateTime.now());
        banner.setUpdatedAt(LocalDateTime.now());
        save(banner);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBanner(Long id, BannerCreateDTO dto) {
        Banner banner = getById(id);
        if (banner == null) {
            throw new ServiceException("Banner 不存在");
        }
        BeanUtils.copyProperties(dto, banner);
        banner.setId(id);
        banner.setUpdatedAt(LocalDateTime.now());
        updateById(banner);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBanner(Long id) {
        if (getById(id) == null) {
            throw new ServiceException("Banner 不存在");
        }
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id, Integer status) {
        Banner banner = getById(id);
        if (banner == null) {
            throw new ServiceException("Banner 不存在");
        }
        banner.setStatus(status);
        banner.setUpdatedAt(LocalDateTime.now());
        updateById(banner);
    }

}