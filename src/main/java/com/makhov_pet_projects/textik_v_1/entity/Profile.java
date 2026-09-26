package com.makhov_pet_projects.textik_v_1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "profiles")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Profile {

	@Id
	@Column(name = "user_id")
	@EqualsAndHashCode.Include
	private Long userId;

	@Column(name = "about", columnDefinition = "text")
	private String about;

	@Column(name = "interests", columnDefinition = "text")
	private String interests;

	@Column(name = "level", nullable = false, length = 2)
	private String level;

	public static Profile of(Long userId, String about, String interests, String level) {
		Profile profile = new Profile();
		profile.setUserId(userId);
		profile.setAbout(about);
		profile.setInterests(interests);
		profile.setLevel(level);
		return profile;
	}

}
