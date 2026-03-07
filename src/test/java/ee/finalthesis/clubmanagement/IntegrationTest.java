package ee.finalthesis.clubmanagement;

import ee.finalthesis.clubmanagement.config.EmbeddedSQL;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = ClubManagementApplication.class)
@EmbeddedSQL
public @interface IntegrationTest {}
