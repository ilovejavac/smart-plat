package com.ware4u.skill.adapt.persist;

import com.dev.lib.entity.dsl.DslQuery;
import com.dev.lib.jpa.entity.BaseRepository;
import lombok.Getter;
import lombok.Setter;

public interface SkillJpaMapper extends BaseRepository<SkillJpaEntity> {

	@Getter
	@Setter
	class LoadSKill extends DslQuery<SkillJpaEntity> {

	}

}
