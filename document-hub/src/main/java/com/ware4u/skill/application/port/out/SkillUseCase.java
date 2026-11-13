package com.ware4u.skill.application.port.out;

import com.dev.lib.web.model.QueryRequest;
import com.ware4u.skill.application.command.CreateSkillCmd;
import com.ware4u.skill.application.command.UpdateSkillCmd;
import com.ware4u.skill.application.dto.SkillView;
import com.ware4u.skill.application.query.QuerySkill;
import org.springframework.data.domain.Page;

public interface SkillUseCase {

	String create(String refrence, CreateSkillCmd command);

	void update(String skillId, String refrence, UpdateSkillCmd command);

	void delete(String skillId);

	void active(String skillId);

	void deactive(String skillId);

	SkillView get(String skillId);

	Page<SkillView> list(QueryRequest<QuerySkill> request);

}
