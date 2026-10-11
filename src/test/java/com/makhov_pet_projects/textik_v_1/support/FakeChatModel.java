package com.makhov_pet_projects.textik_v_1.support;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import lombok.experimental.UtilityClass;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

@UtilityClass
public class FakeChatModel {

	public ChatModel answeringWith(String answer) {
		return prompt -> new ChatResponse(List.of(new Generation(new AssistantMessage(answer))));
	}

	public ChatModel answeringWithSequence(List<String> answers) {
		AtomicInteger calls = new AtomicInteger();
		return prompt -> {
			int index = Math.min(calls.getAndIncrement(), answers.size() - 1);
			return new ChatResponse(List.of(new Generation(new AssistantMessage(answers.get(index)))));
		};
	}

	public ChatModel recording(AtomicReference<Prompt> recorded, String answer) {
		return prompt -> {
			recorded.set(prompt);
			return new ChatResponse(List.of(new Generation(new AssistantMessage(answer))));
		};
	}

	public ChatModel failing() {
		return prompt -> {
			throw new IllegalStateException("endpoint недоступен");
		};
	}

}