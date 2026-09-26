package com.makhov_pet_projects.textik_v_1.domain;

public enum SessionStatus {

	PROPOSED,
	READ,
	THINK,
	DISCUSS,
	DONE;

	public SessionStatus next() {
		return switch (this) {
			case PROPOSED -> READ;
			case READ -> THINK;
			case THINK -> DISCUSS;
			case DISCUSS -> DONE;
			case DONE -> DONE;
		};
	}

}
