package com.vincent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.vincent.common.exception.ServiceException;
import com.vincent.dto.MemberUpdateDTO;
import com.vincent.entity.Member;
import com.vincent.mapper.MemberMapper;
import com.vincent.service.MemberService;
import com.vincent.vo.MemberVO;
import com.vincent.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberServiceImpl extends ServiceImpl<MemberMapper, Member> implements MemberService {

    private final MemberMapper memberMapper;

    @Override
    public List<MemberVO> listAll() {
        List<Member> list = memberMapper.selectList(
                new LambdaQueryWrapper<Member>().orderByDesc(Member::getCreatedAt)
        );
        return list.stream().map(m -> {
            MemberVO vo = new MemberVO();
            BeanUtils.copyProperties(m, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public PageVO<MemberVO> pageQuery(String keyword, Integer status, Integer pageNum, Integer pageSize) {
        Page<Member> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Member> wrapper = new LambdaQueryWrapper<Member>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(Member::getNickname, keyword)
                        .or()
                        .like(Member::getPhone, keyword)
                )
                .eq(status != null, Member::getStatus, status)
                .orderByDesc(Member::getCreatedAt);

        Page<Member> result = page(page, wrapper);

        List<MemberVO> voList = result.getRecords().stream().map(m -> {
            MemberVO vo = new MemberVO();
            BeanUtils.copyProperties(m, vo);
            return vo;
        }).collect(Collectors.toList());

        return new PageVO<>(result.getTotal(), pageNum, pageSize, voList);
    }

    @Override
    public MemberVO getMemberDetail(Long id) {
        Member member = getById(id);
        if (member == null) throw new ServiceException("会员不存在");
        MemberVO vo = new MemberVO();
        BeanUtils.copyProperties(member, vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMember(MemberUpdateDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new ServiceException("会员不存在");
        }
        Member member = getById(dto.getId());
        if (member == null) {
            throw new ServiceException("会员不存在");
        }
        if (dto.getBirthday() != null && dto.getBirthday().isAfter(LocalDate.now())) {
            throw new ServiceException("生日不能是未来的日期");
        }

        // 用 UpdateWrapper 而不是「copyProperties + updateById」：
        // updateById 会忽略 null 字段，那样传 null 就清不掉生日了。
        LambdaUpdateWrapper<Member> update = new LambdaUpdateWrapper<Member>()
                .eq(Member::getId, member.getId())
                .set(Member::getUpdatedAt, LocalDateTime.now());
        if (StringUtils.hasText(dto.getNickname())) {
            update.set(Member::getNickname, dto.getNickname().trim());
        }
        if (StringUtils.hasText(dto.getAvatar())) {
            update.set(Member::getAvatar, dto.getAvatar().trim());
        }
        if (StringUtils.hasText(dto.getPhone())) {
            update.set(Member::getPhone, dto.getPhone().trim());
        }
        // 生日是全量语义：传了就是设成它，传 null 就是清除
        if (dto.getBirthday() != null) {
            update.set(Member::getBirthday, dto.getBirthday());
        } else {
            update.setSql("birthday = NULL");
        }
        memberMapper.update(null, update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleStatus(Long id, Integer status) {
        Member member = getById(id);
        if (member == null) throw new ServiceException("会员不存在");
        member.setStatus(status);
        updateById(member);
    }

}