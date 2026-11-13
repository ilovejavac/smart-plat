package com.ware4u.skill.domain.model;

import com.dev.lib.domain.AggregateRoot;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class Skill extends AggregateRoot {

	private String description;

	private String name;

	private SkillStatus status;

	private String storageRef;

	public static Skill create(String name, String description, String storageRef) {

		return new Skill().setName(name)
				.setDescription(description)
				.setStorageRef(storageRef)
				.setStatus(SkillStatus.active);
	}

	public void active() {

		this.status = SkillStatus.active;
	}

	public void inactive() {

		this.status = SkillStatus.inactive;
	}

	public void deprecated() {

		this.status = SkillStatus.deprecated;
	}

}
