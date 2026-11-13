package com.ware4u.skill.application;

import com.dev.lib.web.model.CodeEnums;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SkillError implements CodeEnums {
	skill_not_exists(36001, "skill 不存在")
	;

	private final Integer code;
	private final String message;
}
