package com.makhov_pet_projects.textik_v_1.support;

import com.makhov_pet_projects.textik_v_1.entity.AppUser;
import com.makhov_pet_projects.textik_v_1.entity.Profile;
import com.makhov_pet_projects.textik_v_1.repository.AppUserRepository;
import com.makhov_pet_projects.textik_v_1.repository.ProfileRepository;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.springframework.security.crypto.password.PasswordEncoder;

@UtilityClass
public class TestFixtures {

	public String randomEmail() {
		return UUID.randomUUID() + "@example.com";
	}

	public AppUser user(AppUserRepository users) {
		return users.saveAndFlush(new AppUser(randomEmail(), "Sergey", "bcrypt-hash"));
	}

	public AppUser userWithPassword(AppUserRepository users, PasswordEncoder passwordEncoder, String rawPassword) {
		return users.saveAndFlush(new AppUser(randomEmail(), "Sergey", passwordEncoder.encode(rawPassword)));
	}

	public AppUser userWithProfile(AppUserRepository users, ProfileRepository profiles) {
		AppUser user = user(users);
		profiles.saveAndFlush(Profile.of(user.getId(), "Люблю IT и спорт", "technology, sport", "B1"));
		return user;
	}

}
