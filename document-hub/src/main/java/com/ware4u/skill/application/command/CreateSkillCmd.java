package com.ware4u.skill.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSkillCmd {

	/**
	 * skill-名称
	 */
	@NotBlank
	public String name;

	/**
	 * skill-描述
	 */
	@NotBlank
	public String description;

}
