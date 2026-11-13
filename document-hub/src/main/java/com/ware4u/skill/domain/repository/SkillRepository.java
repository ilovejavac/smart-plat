package com.ware4u.skill.domain.repository;

import com.ware4u.skill.domain.model.Skill;

import java.util.Optional;

public interface SkillRepository {

	Optional<Skill> findById(String id);

	Skill save(Skill skill);

	void delete(Skill skill);

}
