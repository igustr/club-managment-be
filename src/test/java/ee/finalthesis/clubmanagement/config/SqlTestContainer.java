package ee.finalthesis.clubmanagement.config;

import org.springframework.beans.factory.InitializingBean;
import org.testcontainers.containers.JdbcDatabaseContainer;

public interface SqlTestContainer extends InitializingBean {
  JdbcDatabaseContainer<?> getTestContainer();
}
