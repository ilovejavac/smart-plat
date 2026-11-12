package com.ware4u.service;

import com.dev.lib.security.model.EndpointPermission;
import com.dev.lib.security.service.AuthenticateService;
import com.dev.lib.security.util.UserDetails;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Component
@DubboService
@RequiredArgsConstructor
public class AuthenticateServiceImpl implements AuthenticateService {

	@Override
	public UserDetails loadUserById(Long id) {

		return null;
	}

	@Override
	public Collection<UserDetails> batchLoadUserByIds(Set<Long> ids) {

		return List.of();
	}

	@Override
	public void registerPermissions(List<EndpointPermission> permissions) {

	}

}
