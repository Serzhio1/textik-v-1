package com.makhov_pet_projects.textik_v_1.service;

import com.makhov_pet_projects.textik_v_1.dto.ProfileForm;
import com.makhov_pet_projects.textik_v_1.entity.Profile;
import com.makhov_pet_projects.textik_v_1.repository.ProfileRepository;
import com.makhov_pet_projects.textik_v_1.utility.Interests;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

	private final ProfileRepository profiles;

	@Transactional(readOnly = true)
	public boolean exists(Long userId) {
		return profiles.existsById(userId);
	}

	@Transactional(readOnly = true)
	public Optional<Profile> find(Long userId) {
		return profiles.findById(userId);
	}

	@Transactional
	public Profile create(Long userId, ProfileForm form) {
		return profiles.save(Profile.of(userId, form.about(), Interests.normalize(form.interests()), form.level()));
	}

	@Transactional
	public Optional<Profile> update(Long userId, ProfileForm form) {
		return profiles.findById(userId)
				.map(profile -> {
					profile.setAbout(form.about());
					profile.setInterests(Interests.normalize(form.interests()));
					profile.setLevel(form.level());
					return profile;
				});
	}

}
