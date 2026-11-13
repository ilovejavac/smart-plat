package com.ware4u.skill.adapt.persist;

import com.dev.lib.util.StringUtils;
import com.dev.lib.web.model.QueryRequest;
import com.ware4u.skill.application.dto.SkillView;
import com.ware4u.skill.application.port.in.SkillQueryPort;
import com.ware4u.skill.application.query.QuerySkill;
import com.ware4u.skill.domain.model.SkillToSkillViewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SkillQueryAdapt implements SkillQueryPort {

	private final SkillJpaMapper mapper;

	private final SkillJpaEntityToSkillMapper map2skill;

	private final SkillToSkillViewMapper map2view;

	@Override
	public Page<SkillView> search(QueryRequest<QuerySkill> request) {

		return mapper.page(new SkillJpaMapper.LoadSKill().external(request))
				.map(map2skill::convert)
				.map(map2view::convert);
	}

	@Override
	public Optional<SkillView> findById(String id) {

		return mapper.load(new SkillJpaMapper.LoadSKill().setBizId(id))
				.map(map2skill::convert)
				.map(map2view::convert);
	}

}
