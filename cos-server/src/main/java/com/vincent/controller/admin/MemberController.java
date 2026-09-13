package com.vincent.controller.admin;

import com.vincent.common.BaseContext;
import com.vincent.common.Result;
import com.vincent.dto.MemberUpdateDTO;
import com.vincent.service.MemberService;
import com.vincent.vo.MemberVO;
import com.vincent.vo.PageVO;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController("adminMemberController")
@RequestMapping("/admin/member")
@RequiredArgsConstructor
@Slf4j
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/list")
    public Result<List<MemberVO>> list() {
        return Result.success(memberService.listAll());
    }

    @GetMapping("/page")
    public Result<PageVO<MemberVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(memberService.pageQuery(keyword, status, page, pageSize));
    }

    @GetMapping("/current")
    public Result<MemberVO> current() {
        return Result.success(memberService.getMemberDetail(BaseContext.getCurrentId()));
    }

    @GetMapping("/{id}")
    public Result<MemberVO> detail(@PathVariable Long id) {
        return Result.success(memberService.getMemberDetail(id));
    }

    @PutMapping
    public Result<Void> update(@RequestBody MemberUpdateDTO dto) {
        memberService.updateMember(dto);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        memberService.toggleStatus(id, status);
        return Result.success();
    }

}