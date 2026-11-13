package com.ware4u.skill.adapt.persist;

import com.dev.lib.jpa.entity.JpaEntity;
import com.ware4u.skill.domain.model.Skill;
import com.ware4u.skill.domain.model.SkillStatus;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMappers;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "skills")
@AutoMappers({
		@AutoMapper(target = Skill.class)
})
public class SkillJpaEntity extends JpaEntity {

	private String description;

	private String name;

	@Enumerated(EnumType.STRING)
	@Column(length = 24, columnDefinition = "varchar(24)")
	private SkillStatus status;

	@Column(unique = true, nullable = false)
	private String storageRef;

}
