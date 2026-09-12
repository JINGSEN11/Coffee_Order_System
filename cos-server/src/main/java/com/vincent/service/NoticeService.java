package com.vincent.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.vincent.dto.NoticeCreateDTO;
import com.vincent.entity.Notice;
import com.vincent.vo.NoticeVO;
import com.vincent.vo.PageVO;

public interface NoticeService extends IService<Notice> {

    PageVO<NoticeVO> pageQuery(String title, Integer status, Integer page, Integer pageSize);

    NoticeVO getNoticeDetail(Long id);

    void createNotice(NoticeCreateDTO dto);

    void updateNotice(Long id, NoticeCreateDTO dto);

    void deleteNotice(Long id);

}