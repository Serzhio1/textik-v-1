package com.makhov_pet_projects.textik_v_1.service;

import com.makhov_pet_projects.textik_v_1.entity.AppUser;
import com.makhov_pet_projects.textik_v_1.exceptions.EmailAlreadyTakenException;
import com.makhov_pet_projects.textik_v_1.repository.AppUserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserRegistrationService {

	private final AppUserRepository users;
	private final PasswordEncoder passwordEncoder;

	public AppUser register(String email, String username, String rawPassword) {
		String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
		if (users.existsByEmailIgnoreCase(normalizedEmail)) {
			throw new EmailAlreadyTakenException(normalizedEmail);
		}
		AppUser user = new AppUser(normalizedEmail, username.trim(), passwordEncoder.encode(rawPassword));
		try {
			return users.saveAndFlush(user);
		} catch (DataIntegrityViolationException e) {
			throw new EmailAlreadyTakenException(normalizedEmail);
		}
	}

}
