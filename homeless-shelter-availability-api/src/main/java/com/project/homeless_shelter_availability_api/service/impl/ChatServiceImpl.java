package com.project.homeless_shelter_availability_api.service.impl;

import com.project.homeless_shelter_availability_api.model.Shelter;
import com.project.homeless_shelter_availability_api.service.ChatService;
import com.project.homeless_shelter_availability_api.service.ShelterService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ShelterService shelterService;
    private final WebClient.Builder webClientBuilder;

    @Value("${app.openrouter.api-key:}")
    private String openRouterApiKey;

    @Value("${app.openrouter.model:meta-llama/llama-3.1-8b-instruct:free}")
    private String openRouterModel;

    @Value("${app.openrouter.base-url:https://openrouter.ai/api/v1/chat/completions}")
    private String openRouterBaseUrl;

    @Override
    public String chat(String message) {

        if (openRouterApiKey == null || openRouterApiKey.isBlank()) {
            return "OpenRouter API key is not configured.";
        }

        List<Shelter> shelters = shelterService.getAllShelters();
        String prompt = buildPrompt(message, shelters);

        try {
            Map<?, ?> response = webClientBuilder.build()
                    .post()
                    .uri(openRouterBaseUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openRouterApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                            "model", openRouterModel,
                            "messages", List.of(Map.of("role", "user", "content", prompt))
                    ))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return extractText(response);
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                return "OpenRouter is rate-limiting requests right now. Please try again in a minute.";
            }
            return "OpenRouter request failed with status " + e.getStatusCode().value() + ".";
        } catch (Exception e) {
            return "Unable to reach OpenRouter right now. Please try again shortly.";
        }
    }

    private String buildPrompt(String message, List<Shelter> shelters) {
        String shelterContext = shelters.stream()
                .limit(20)
                .map(this::formatShelter)
                .collect(Collectors.joining("\n"));

        return "You are a shelter assistant. Use only the shelter data provided. "
                + "If the answer is not in the data, say that you do not know.\n\n"
                + "If the user asks a question or makes a statement that is off topic, politely state your purpose as an assistant and redirect them to an appropriate query example.\n\n"
                + "User question: " + (message == null ? "" : message) + "\n\n"
                + "Shelter data:\n" + shelterContext;
    }

    private String formatShelter(Shelter shelter) {
        return "- Name: " + valueOrUnknown(shelter.getName())
                + ", City: " + valueOrUnknown(shelter.getCity())
                + ", State: " + valueOrUnknown(shelter.getState())
                + ", Address: " + valueOrUnknown(shelter.getAddress())
                + ", Available beds: " + valueOrUnknown(shelter.getAvailableBeds())
                + ", Description: " + valueOrUnknown(shelter.getDescription());
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<?, ?> response) {
        if (response == null) {
            return "No response from OpenRouter.";
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            return "OpenRouter returned no choices.";
        }

        Map<String, Object> firstChoice = choices.getFirst();
        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
        if (message == null) {
            return "OpenRouter returned no message content.";
        }

        Object content = message.get("content");
        return content == null ? "OpenRouter returned an empty response." : content.toString();
    }

    private String valueOrUnknown(Object value) {
        return value == null ? "unknown" : value.toString();
    }
}
