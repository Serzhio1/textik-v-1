package com.makhov_pet_projects.textik_v_1.service;

import com.makhov_pet_projects.textik_v_1.dto.GeneratedQuestion;
import com.makhov_pet_projects.textik_v_1.dto.GeneratedText;
import com.makhov_pet_projects.textik_v_1.dto.ReviewData;
import com.makhov_pet_projects.textik_v_1.dto.TopicProposal;
import com.makhov_pet_projects.textik_v_1.entity.LearningSession;
import com.makhov_pet_projects.textik_v_1.entity.Profile;
import java.util.List;

public interface AiGateway {

	List<TopicProposal> generateTopics(Profile profile);

	GeneratedText generateText(TopicProposal topic, Profile profile);

	List<GeneratedQuestion> generateQuestions(GeneratedText text, Profile profile);

	String continueDiscussion(LearningSession session, String userMessage);

	ReviewData generateReview(LearningSession session);

}