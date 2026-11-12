package com.ware4u.api;

import com.dev.lib.web.model.ServerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController {

	@PostMapping("/api/login")
	public ServerResponse<Void> login() {

		return ServerResponse.ok();
	}

	@PostMapping("/api/logout")
	public ServerResponse<Void> logout() {

		return ServerResponse.ok();
	}

	@PostMapping("/api/refresh-token")
	public ServerResponse<Void> refresh() {

		return ServerResponse.ok();
	}

	@PostMapping("/api/parse-token")
	public ServerResponse<Void> parseToken() {

		return ServerResponse.ok();
	}

	@GetMapping("/api/user-info")
	public ServerResponse<Void> getUserInfo() {

		return ServerResponse.ok();
	}

}
