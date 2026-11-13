package com.ware4u.skill.application.dto;

import com.dev.lib.web.BaseVO;
import com.ware4u.skill.domain.model.Skill;
import com.ware4u.skill.domain.model.SkillStatus;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMappers;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AutoMappers({
		@AutoMapper(target = Skill.class, convertGenerate = false)
})
public class SkillView extends BaseVO {

	private String description;

	private String name;

	private SkillStatus status;

}
