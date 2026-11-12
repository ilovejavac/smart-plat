package com.ware4u.api;

import com.dev.lib.storage.Storage;
import com.dev.lib.storage.config.AppStorageProperties;
import com.dev.lib.web.model.ServerResponse;
import com.ware4u.api.request.CreateSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FileController implements InitializingBean {

	private String bucket;

	private final AppStorageProperties properties;

	@PostMapping("/api/files/upload-session")
	public ServerResponse<Void> uploadSession(@RequestBody CreateSession body) {

		Storage.bucket(bucket).object("").presignedUploadUrl(3600, null);

		return ServerResponse.ok();
	}

	@PostMapping("/api/files/{id}/completed")
	public ServerResponse<Void> complete() {

		return ServerResponse.ok();
	}

	@GetMapping("/api/files/download-url")
	public ServerResponse<Void> downloads() {

		return ServerResponse.ok();
	}

	@GetMapping("/api/files/preview-url")
	public ServerResponse<Void> preview() {

		return ServerResponse.ok();
	}

	@GetMapping("/api/files/metadata")
	public ServerResponse<Void> metafile() {

		return ServerResponse.ok();
	}

	@PostMapping("/api/files/remove")
	public ServerResponse<Void> remove() {

		return ServerResponse.ok();
	}

	@Override
	public void afterPropertiesSet() {

		this.bucket = properties.getRustfs().getBucket();
	}

}
