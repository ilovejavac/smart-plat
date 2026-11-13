package com.ware4u.skill.api;

import com.dev.lib.security.service.annotation.Anonymous;
import com.dev.lib.web.model.ServerResponse;
import com.ware4u.skill.application.command.CreateSkillCmd;
import com.ware4u.skill.application.port.out.SkillUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Anonymous
@RestController
@RequiredArgsConstructor
public class SkillController {

	private final SkillUseCase skills;

	/**
	 * 创建 skill
	 *
	 * @param command  body
	 * @param refrence skill文件引用
	 * @return {@link ServerResponse }<{@link String }>
	 */
	@PostMapping("/api/skills")
	public ServerResponse<String> create(@RequestBody @Valid CreateSkillCmd command,
										 @NotBlank @RequestParam String refrence) {

		return ServerResponse.success(skills.create(refrence, command));
	}

	/**
	 * 删除 skill
	 *
	 * @param id skill-id
	 * @return {@link ServerResponse }<{@link Void }>
	 */
	@DeleteMapping("/api/skills/{id}")
	public ServerResponse<Void> delete(@PathVariable String id) {

		skills.delete(id);
		return ServerResponse.ok();
	}

}
