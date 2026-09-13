package com.vincent.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.vincent.dto.MemberUpdateDTO;
import com.vincent.entity.Member;
import com.vincent.vo.MemberVO;
import com.vincent.vo.PageVO;

public interface MemberService extends IService<Member> {

    PageVO<MemberVO> pageQuery(String keyword, Integer status, Integer page, Integer pageSize);

    MemberVO getMemberDetail(Long id);

    void updateMember(MemberUpdateDTO dto);

    void toggleStatus(Long id, Integer status);
}