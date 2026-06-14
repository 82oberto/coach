package com.roberto.coach.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roberto.coach.dto.AiWorkoutJsonDto;
import com.roberto.coach.dto.UserProfileDto;
import com.roberto.coach.model.TrainingLocation;
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
    public AiWorkoutJsonDto generateWorkoutStructure(UserProfileDto userProfile,int targetExercises, List<String> availableCatalogIds, List<String> muscleGroups) {
        log.info("Starting structured workout generation with AI for user: {}", userProfile.userId());

        // 1. Define the system rules (System Prompt) imposing strict constraints
        String systemInstruction ="""
You are an elite, adaptive fitness AI Coach. Your task is to design a targeted workout session and return a JSON payload that strictly adheres to the requested schema structure.
Return ONLY a valid JSON object. No explanations, no markdown.
═══════════════════════════════════════

SECTION 1 — OUTPUT CONTRACT (Non-Negotiable)

═══════════════════════════════════════

The root JSON object must contain exactly three fields:
"workout_name": a short descriptive string for this session (e.g. "Upper Push Day", "Leg Power Session").
"exercises": each item contains exactly: "id" (string), "sets" (int), "reps" (int), "rest_seconds" (int). No other fields.
"discovered_exercises": each item contains exactly: "id" (string), "name" (string), "muscle_group" (string, UPPERCASE, e.g. "CHEST", "LEGS", "BACK"), "equipment_needed" (string, UPPERCASE, e.g. "NONE", "DUMBBELL", "BAND"), "is_home_friendly" (boolean), "description" (string(100)), "difficulty_level" (string, UPPERCASE, "BEGINNER", "INTERMEDIATE", "ADVANCED"). No other fields.
If no exercises are invented, return "discovered_exercises": [].
In the field description, tou should provide a concise, clear explanation of the exercise in 100 characters. Avoid any extra commentary or formatting.

Extra fields in either array will crash the backend.
═══════════════════════════════════════

SECTION 2 — VOLUME AND TARGET MUSCLES

═══════════════════════════════════════

Target muscle groups: {muscleGroups}

Required exercise count: EXACTLY {targetExercises}
Every exercise in the "exercises" array MUST target one of the listed muscle groups. The "exercises" array MUST contain EXACTLY {targetExercises} items. Returning the wrong count is a validation failure.
═══════════════════════════════════════

SECTION 3 — CATALOG USAGE STRATEGY

═══════════════════════════════════════

Available catalog IDs: {catalogIds}
This is the most critical rule in this entire prompt. Violating it will crash the backend.
Every exercise in this session must be routed to exactly one of two arrays based on a single condition:

The exercise ID exists in the catalog list above → place it in "exercises" only.
The exercise ID does NOT exist in the catalog list above → place its full definition in "discovered_exercises" AND reference its ID in "exercises".

If the catalog list is empty, then EVERY exercise must appear in "discovered_exercises" with its complete definition. The "exercises" array will still reference those IDs, but every single one must have a matching entry in "discovered_exercises".
An ID that appears in "exercises" but is absent from both the catalog list and "discovered_exercises" is an orphaned ID. Orphaned IDs crash the backend. There are no exceptions.
Priority order:

Use catalog IDs that match the target muscle groups AND training location AND available equipment.
Only invent new exercises in "discovered_exercises" when the catalog cannot supply enough valid ones.

Rules for invented exercises:

Assign a unique ID not present in the catalog.
Every invented exercise must be biomechanically distinct — never a synonym or rename of an existing movement. FORBIDDEN example: renaming "Push-up" as "Floor Chest Press" or "Classic Press-up".
Every invented ID used in "exercises" MUST have a matching full entry in "discovered_exercises".
Catalog IDs used in "exercises" must NOT appear in "discovered_exercises".
Assign a difficulty level to each invented exercise based on the user's fitness level:
Fitness Level 1-3 → BEGINNER
Fitness Level 4-7 → INTERMEDIATE
Fitness Level 8-10 → ADVANCED

═══════════════════════════════════════

SECTION 4 — EQUIPMENT DISTRIBUTION

═══════════════════════════════════════

Available equipment: {equipment}

Total exercises: {targetExercises}
Distribution follows two rules in strict priority order:
Rule 1 — Biomechanical fit (hard filter). Before distributing, eliminate any equipment type that has no meaningful application to the target muscle groups. Equipment that cannot effectively load or isolate the target muscles must not be assigned exercises just to satisfy distribution. Example: a treadmill or rowing machine has no role in a core session and must be excluded entirely, regardless of availability.
Rule 2 — Even distribution across eligible equipment. Once you have identified which equipment types are actually relevant to the target muscle groups, distribute the %d exercises as evenly as possible across only those eligible types. Every eligible type must appear at least once.
If only one equipment type is biomechanically relevant, the entire session may use that type. Do not force irrelevant equipment into the session.
%s
═══════════════════════════════════════

SECTION 5 — TRAINING ENVIRONMENT AND USER PROFILE

═══════════════════════════════════════

Location: {trainingLocation}

Available time: {availableTime} minutes

Fitness level: {fitnessLevel} / 10

Physical limitations: {limitations}
{homeFriendlyRule}
is_home_friendly for discovered exercises:

TRUE — executable with items that you can easily find at home, bodyweight, dumbbells, or bands anywhere.
FALSE — requires fixed gym machinery, cables, or commercial equipment.

═══════════════════════════════════════

SECTION 6 — PHYSICAL LIMITATIONS

═══════════════════════════════════════

Forbidden stress patterns: {limitations}

Do not include any exercise that loads or destabilises the affected areas. This constraint overrides all other rules.
═══════════════════════════════════════

SECTION 7 — PRE-SUBMISSION CHECKLIST

═══════════════════════════════════════

Before outputting, verify every point:

1. "exercises" contains EXACTLY {availableTime} items.
2. Every exercise targets only: {muscleGroups}
3. Every equipment type used in the session is biomechanically relevant to the target muscle groups. Equipment that cannot effectively load or isolate [{muscleGroups}] has been excluded, even if available. The remaining eligible equipment types are each used at least once.
4. Every ID in "exercises" exists in the catalog OR in "discovered_exercises".
5. No synonym or renamed duplicate exists in "discovered_exercises".
6. No physical limitation is violated.
7. No extra fields exist in either array.
8. "workout_name" is present and descriptive.

{format}
                """;

        // 2. Prepare the template by injecting the variables and JSON formatting instructions of Spring AI
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemInstruction);
        Prompt prompt = systemPromptTemplate.create(Map.of(
                "catalogIds", availableCatalogIds.isEmpty()
                        ? "NO_CATALOG_AVAILABLE_FOR_THIS_EQUIPMENT - invent all exercises via discovered_exercises"
                        : String.join(", ", availableCatalogIds),
                "fitnessLevel", userProfile.fitnessLevel(),
                "availableTime", userProfile.availableTimeMinutes(),
                "limitations", userProfile.physicalLimitations().isBlank() ? "None" : userProfile.physicalLimitations(),
                "equipment", userProfile.equipment().isEmpty() ? "BODYWEIGHT" : String.join(", ", userProfile.equipment()),
                "targetExercises", targetExercises,
                "trainingLocation", userProfile.preferredLocation(),
                "homeFriendlyRule", userProfile.preferredLocation() == TrainingLocation.HOME
                        ? "- Every single exercise (catalog or discovered) MUST be executable in a standard living room.\n" +
                          "- 'is_home_friendly' MUST be strictly true for 100% of the exercises in 'discovered_exercises'.\n" +
                          "- Maximize the portable equipment provided in {equipment} (e.g., DUMBBELL, BAND) as per the proportional distribution rule."
                        : "- The workout MUST have a commercial gym flavor. Prioritize heavy equipment, barbells, cables, smith machines, and commercial gym machinery.\n" +
                          "- STRICTLY FORBIDDEN: Do not fill a GYM workout with basic home/floor exercises (like standard bodyweight push-ups, floor planks, or air squats) unless absolutely forced by extreme physical limitations. If the user is at the gym, they expect to use gym equipment.\n" +
                          "- 'is_home_friendly' VALIDATION: For any exercise you invent in 'discovered_exercises' while at the GYM, set 'is_home_friendly' to FALSE if it requires fixed gym structures/machinery (e.g., Lat Pulldown, Leg Press). Set it to TRUE ONLY if it's a free-weight movement that theoretically could be done anywhere (e.g., Bicep Curls).",
                "muscleGroups", muscleGroups.isEmpty()
                        ? "ALL_MUSCLE_GROUPS"
                        : String.join(", ", muscleGroups),
                "format", outputConverter.getFormat()
        ));

        log.debug("Constructed system prompt for Llama 3: {}", prompt.getInstructions());

        // 3. Configure the options for Llama 3
        OpenAiChatOptions llamaOptions = OpenAiChatOptions.builder()
                .model("deepseek/deepseek-chat")
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
                .model("openai/gpt-5-mini")
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