package com.ware4u.skill.application.port.in;

import com.dev.lib.web.model.QueryRequest;
import com.ware4u.skill.application.dto.SkillView;
import com.ware4u.skill.application.query.QuerySkill;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface SkillQueryPort {

	Page<SkillView> search(QueryRequest<QuerySkill> request);

	Optional<SkillView> findById(String id);

}
