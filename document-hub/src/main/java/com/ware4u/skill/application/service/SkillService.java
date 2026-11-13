package com.ware4u.skill.application.service;

import com.dev.lib.storage.service.FileService;
import com.dev.lib.web.model.QueryRequest;
import com.ware4u.skill.application.SkillError;
import com.ware4u.skill.application.SkillException;
import com.ware4u.skill.application.command.CreateSkillCmd;
import com.ware4u.skill.application.command.UpdateSkillCmd;
import com.ware4u.skill.application.dto.SkillView;
import com.ware4u.skill.application.port.in.SkillQueryPort;
import com.ware4u.skill.application.port.out.SkillUseCase;
import com.ware4u.skill.application.query.QuerySkill;
import com.ware4u.skill.domain.model.Skill;
import com.ware4u.skill.domain.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SkillService implements SkillUseCase {

	private final SkillRepository skills;

	private final SkillQueryPort queries;

	private final FileService files;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public String create(String refrence, CreateSkillCmd command) {

		Skill skill = Skill.create(command.getName(), command.getDescription(), refrence);

		return skills.save(skill).getBizId();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(String skillId, String refrence, UpdateSkillCmd command) {

	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void delete(String skillId) {

		Skill skill = load(skillId);
		skill.deprecated();

		skills.delete(skill);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void active(String skillId) {

		Skill skill = load(skillId);
		skill.active();

		skills.save(skill);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deactive(String skillId) {

		Skill skill = load(skillId);
		skill.inactive();

		skills.save(skill);
	}

	@Override
	@Transactional(readOnly = true)
	public SkillView get(String skillId) {

		return queries.findById(skillId)
				.orElseThrow(() -> new SkillException(SkillError.skill_not_exists));
	}

	@Override
	@Transactional(readOnly = true)
	public Page<SkillView> list(QueryRequest<QuerySkill> request) {

		return queries.search(request);
	}

	private Skill load(String id) {

		Skill skill = skills.findById(id)
				.orElseThrow(() -> new SkillException(SkillError.skill_not_exists));
		skill.isOwner();
		return skill;
	}

}
