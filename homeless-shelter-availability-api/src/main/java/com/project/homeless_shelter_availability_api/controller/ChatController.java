package com.project.homeless_shelter_availability_api.controller;

import com.project.homeless_shelter_availability_api.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

	private final ChatService chatService;

	@PostMapping
	public ResponseEntity<String> chat(@RequestBody(required = false) String message) {
		return ResponseEntity.ok(chatService.chat(message));
	}
}

