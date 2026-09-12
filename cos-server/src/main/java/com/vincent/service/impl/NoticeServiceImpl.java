package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.vincent.common.exception.ServiceException;
import com.vincent.dto.NoticeCreateDTO;
import com.vincent.entity.Notice;
import com.vincent.mapper.NoticeMapper;
import com.vincent.service.NoticeService;
import com.vincent.vo.NoticeVO;
import com.vincent.vo.PageVO;
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
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {

    private final NoticeMapper noticeMapper;

    @Override
    public PageVO<NoticeVO> pageQuery(String title, Integer status, Integer pageNum, Integer pageSize) {
        Page<Notice> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<Notice>()
                .like(StringUtils.hasText(title), Notice::getTitle, title)
                .eq(status != null, Notice::getStatus, status)
                .orderByDesc(Notice::getCreatedAt);

        Page<Notice> result = page(page, wrapper);

        List<NoticeVO> voList = result.getRecords().stream().map(notice -> {
            NoticeVO vo = new NoticeVO();
            BeanUtils.copyProperties(notice, vo);
            return vo;
        }).collect(Collectors.toList());

        return new PageVO<>(result.getTotal(), pageNum, pageSize, voList);
    }

    @Override
    public NoticeVO getNoticeDetail(Long id) {
        Notice notice = getById(id);
        if (notice == null) {
            throw new ServiceException("公告不存在");
        }
        NoticeVO vo = new NoticeVO();
        BeanUtils.copyProperties(notice, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createNotice(NoticeCreateDTO dto) {
        Notice notice = new Notice();
        BeanUtils.copyProperties(dto, notice);
        notice.setCreatedAt(LocalDateTime.now());
        notice.setUpdatedAt(LocalDateTime.now());
        save(notice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNotice(Long id, NoticeCreateDTO dto) {
        Notice notice = getById(id);
        if (notice == null) {
            throw new ServiceException("公告不存在");
        }
        BeanUtils.copyProperties(dto, notice);
        notice.setId(id);
        notice.setUpdatedAt(LocalDateTime.now());
        updateById(notice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotice(Long id) {
        if (getById(id) == null) {
            throw new ServiceException("公告不存在");
        }
        removeById(id);
    }

}