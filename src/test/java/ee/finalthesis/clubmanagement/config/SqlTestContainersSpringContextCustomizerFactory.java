package ee.finalthesis.clubmanagement.config;

import java.util.List;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

public class SqlTestContainersSpringContextCustomizerFactory implements ContextCustomizerFactory {

  private static SqlTestContainer prodTestContainer;

  @Override
  public ContextCustomizer createContextCustomizer(
      Class<?> testClass, List<ContextConfigurationAttributes> configAttributes) {
    return (ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) -> {
      EmbeddedSQL sqlAnnotation =
          AnnotatedElementUtils.findMergedAnnotation(testClass, EmbeddedSQL.class);
      if (null != sqlAnnotation) {
        if (null == prodTestContainer) {
          try {
            prodTestContainer =
                context
                    .getBeanFactory()
                    .createBean(
                        (Class<? extends SqlTestContainer>)
                            Class.forName(PostgreSqlTestContainer.class.getName()));
          } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
          }
        }
        TestPropertyValues.of(
                "spring.datasource.url=" + prodTestContainer.getTestContainer().getJdbcUrl(),
                "spring.datasource.username=" + prodTestContainer.getTestContainer().getUsername(),
                "spring.datasource.password=" + prodTestContainer.getTestContainer().getPassword())
            .applyTo(context);
      }
    };
  }
}
