package com.roberto.coach.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roberto.coach.dto.AiWorkoutJsonDto;
import com.roberto.coach.dto.UserProfileDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AiRoutingService {

    private static final Logger log = LoggerFactory.getLogger(AiRoutingService.class);

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    // initialize Spring AI's output converter to enforce the JSON schema for our DTO
    private final BeanOutputConverter<AiWorkoutJsonDto> outputConverter;

    public AiRoutingService(ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.outputConverter = new BeanOutputConverter<>(AiWorkoutJsonDto.class);
    }

    /**
     * Generates the structured workout plan in a JSON format using Llama 3.
     *
     * @param userProfile         The user profile with metrics and physical constraints.
     * @param availableCatalogIds The list of available exercise IDs in the database.
     * @return A validated and typed AiWorkoutJsonDto object.
     */
    public AiWorkoutJsonDto generateWorkoutStructure(UserProfileDto userProfile,int targetExercises, List<String> availableCatalogIds) {
        log.info("Starting structured workout generation with Llama 3 for user: {}", userProfile.userId());

        // 1. Define the system rules (System Prompt) imposing strict constraints
        String systemInstruction ="""
            You are an elite, adaptive fitness AI Coach. Your task is to design a targeted workout session and return a JSON payload that strictly adheres to the requested schema structure.

            CRITICAL STRUCTURE AND ID RULES:
            1. EXERCISE SELECTION: You MUST preferentially choose exercise IDs from this available catalog list: {catalogIds}. Place these references inside the 'exercises' array. If the catalog list is empty or NO_CATALOG_AVAILABLE, you MUST dynamically provision all exercises using the discovered_exercises array
            2. DYNAMIC PROVISIONING: If and ONLY if a specific movement is absolutely necessary for the user's goals but missing from the catalog, you can invent a new unique string ID (e.g., "ex-custom-squat").
            3. NO ORPHANED IDs (ANTI-CRASH RULE): If you use an exercise ID inside the 'exercises' array that is NOT part of the provided {catalogIds} list, you MUST include a single, complete definition for that exercise inside the 'discovered_exercises' array with the EXACT same 'id'.
            4. FIELD VALIDATION:
                - Every item in the 'exercises' array must strictly contain only: 'id', 'sets', 'reps', and 'rest_seconds'.
                - Every item in the 'discovered_exercises' array must strictly contain only: 'id', 'name', 'muscle_group' (UPPERCASE, e.g., "LEGS", "CHEST"), and 'equipment_needed' (UPPERCASE, e.g., "NONE", "DUMBBELL").

            USER PROFILE CONSTRAINTS:
                5. Respect the user's fitness level ({fitnessLevel}/10), training days, and available time ({availableTime} mins).
                CRITICAL VOLUME RULE:
                    Based on the user's available time ({availableTime}), you MUST return EXACTLY {targetExercises} exercises in the 'exercises' array.
                    A workout with less than {targetExercises} or more than {targetExercises} exercises is considered a failure.
                6. Strictly avoid movements that stress these physical limitations: {limitations}.
                7. Use only the following available equipment: {equipment}.

                Before returning the payload, perform a final validation pass: ensure that every single 'id' present in the 'exercises' list is either present in the provided catalog OR has its full metadata declared inside 'discovered_exercises'. Missing this mapping will crash the backend application.

                {format}
                """;

        // 2. Prepare the template by injecting the variables and JSON formatting instructions of Spring AI
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemInstruction);
        Prompt prompt = systemPromptTemplate.create(Map.of(
                "catalogIds", availableCatalogIds.isEmpty()
                        ? "NO_CATALOG_AVAILABLE_FOR_THIS_EQUIPMENT"
                        : String.join(", ", availableCatalogIds),
                "fitnessLevel", userProfile.fitnessLevel(),
                "availableTime", userProfile.availableTimeMinutes(),
                "limitations", userProfile.physicalLimitations().isBlank() ? "None" : userProfile.physicalLimitations(),
                "equipment", userProfile.equipment().isEmpty() ? "BODYWEIGHT" : String.join(", ", userProfile.equipment()),
                "targetExercises", targetExercises,
                "format", outputConverter.getFormat()
        ));

        log.debug("Constructed system prompt for Llama 3: {}", prompt.getInstructions());

        // 3. Configure the options for Llama 3
        OpenAiChatOptions llamaOptions = OpenAiChatOptions.builder()
                .model("meta-llama/llama-3-8b-instruct")
                .temperature(0.1)
                .responseFormat(ResponseFormat.builder()
                        .type(ResponseFormat.Type.JSON_OBJECT)
                        .build())
                .build();

        Prompt finalPrompt = new Prompt(prompt.getInstructions(), llamaOptions);

        // 4. Execute the call to the AI and automatically parse the result
        try {
            String rawJsonResponse = chatModel.call(finalPrompt).getResult().getOutput().getText();
            log.debug("Raw JSON response received from Llama 3: {}", rawJsonResponse);

            // Convert the JSON string directly into our typed Java Record
            return outputConverter.convert(rawJsonResponse);
        } catch (Exception e) {
            log.error("Error during Llama 3 workout generation or parsing", e);
            throw new RuntimeException("Unable to generate a valid workout from the AI. Fallback triggered.", e);
        }
    }

    /**
     * Generates a fluid and empathetic motivational message.
     *
     * @param userProfile The user profile context to personalize the tone.
     * @return A plain text string containing the motivational message from the coach.
     */
    public String generateMotivationalMessage(UserProfileDto userProfile) {
        log.info("Generating motivational message");

        String userContextJson;
        try {
            userContextJson = objectMapper.writeValueAsString(userProfile);
        } catch (JsonProcessingException e) {
            userContextJson = "Fitness Level: " + userProfile.fitnessLevel();
        }

        String promptText = """
            Write a short, engaging, and highly energetic motivational message in English for a fitness enthusiast.
            Tailor the message keeping in mind their profile data: %s.
            Focus on consistency and adapting to their lifestyle. Keep it under 3 sentences. No markdown formatting, just plain text.
            """.formatted(userContextJson);

        OpenAiChatOptions gemmaOptions = OpenAiChatOptions.builder()
                .model("deepseek/deepseek-v4-flash")
                .temperature(0.7)
                .build();

        Prompt finalPrompt = new Prompt(promptText, gemmaOptions);

        try {
            return chatModel.call(finalPrompt).getResult().getOutput().getText().trim();
        } catch (Exception e) {
            log.warn("this model encountered an issue. Returning a default fallback motivational message.", e);
            return "Let's crush today's workout! Consistency is key.";
        }
    }
}