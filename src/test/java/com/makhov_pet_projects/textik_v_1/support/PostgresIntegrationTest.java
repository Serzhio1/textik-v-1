package com.makhov_pet_projects.textik_v_1.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
@ActiveProfiles("test")
public abstract class PostgresIntegrationTest {

}
