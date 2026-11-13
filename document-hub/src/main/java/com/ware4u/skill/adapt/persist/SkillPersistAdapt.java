package com.ware4u.skill.adapt.persist;

import com.ware4u.skill.domain.model.Skill;
import com.ware4u.skill.domain.model.SkillToSkillJpaEntityMapper;
import com.ware4u.skill.domain.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SkillPersistAdapt implements SkillRepository {

	private final SkillJpaMapper mapper;

	private final SkillToSkillJpaEntityMapper map2jpa;

	private final SkillJpaEntityToSkillMapper map2skill;

	@Override
	public Optional<Skill> findById(String id) {

		return mapper.load(new SkillJpaMapper.LoadSKill().setBizId(id)).map(map2skill::convert);
	}

	@Override
	public Skill save(Skill skill) {

		SkillJpaEntity entity = mapper.findById(skill.getId()).orElseGet(SkillJpaEntity::new);

		map2jpa.convert(skill, entity);
		SkillJpaEntity save = mapper.save(entity);
		return map2skill.convert(save);

	}

	@Override
	public void delete(Skill skill) {

		mapper.findById(skill.getId()).ifPresent(it -> map2jpa.convert(skill, it));

		mapper.delete(skill);
	}

}
