package com.makhov_pet_projects.textik_v_1.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
public abstract class WebTest extends PostgresIntegrationTest {

	@Autowired
	protected MockMvc mockMvc;

}
