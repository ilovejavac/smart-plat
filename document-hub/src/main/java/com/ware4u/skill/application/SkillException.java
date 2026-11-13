package com.ware4u.skill.application;

import com.dev.lib.exceptions.BizException;

public class SkillException extends BizException {

	public SkillException(SkillError ce) {

		super(ce);
	}

}
