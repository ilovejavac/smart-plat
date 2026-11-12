package com.ware4u.api.request;

public record CreateSession(
		String filename,
		Long size,
		String contentType
) {
}
