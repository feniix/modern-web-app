package org.knowhow.mwa.morphia;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import javax.validation.ValidatorFactory;

import org.apache.commons.lang3.Validate;
import org.knowhow.mwa.mongo.MongoModule;
import org.knowhow.mwa.validation.ValidationModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.google.code.morphia.AbstractEntityInterceptor;
import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.mapping.Mapper;
import com.google.code.morphia.mapping.validation.ConstraintViolationException;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;

/**
 * <p>
 * Add support for {@link Morphia} on a Mongo database. Clients must provide a
 * instance of {@link MorphiaConfigurer} to be able to detect Morphia persistent
 * classes.
 * </p>
 * <p>
 * Also, it configure a Morphia {@link Datastore} ready to use.
 * </p>
 *
 * @author edgar.espina
 * @see MongoModule
 * @see MorphiaConfigurer
 */
@Configuration
@Import(MongoModule.class)
public class MorphiaModule {

  /**
   * A JSR-303 entity interceptor.
   *
   * @author edgar.espina
   */
  private static final class Jsr303Interceptor extends
      AbstractEntityInterceptor {
    /**
     * The validator factory. Required.
     */
    private ValidatorFactory validatorFactory;

    /**
     * Creates a new {@link Jsr303Interceptor}.
     *
     * @param validatorFactory The validator factory. Required.
     */
    public Jsr303Interceptor(final ValidatorFactory validatorFactory) {
      this.validatorFactory =
          checkNotNull(validatorFactory, "The validator factory is required.");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void prePersist(final Object ent, final DBObject dbObj,
        final Mapper mapr) {
      @SuppressWarnings("rawtypes")
      final Set validate = validatorFactory.getValidator().validate(ent);
      if (!validate.isEmpty()) {
        throw new ConstraintViolationException(validate);
      }
    }
  }

  /**
   * The logging system.
   */
  private static final Logger logger = LoggerFactory
      .getLogger(MorphiaModule.class);

  /**
   * The local validator factory's bean.
   */
  @Autowired(required = false)
  @Qualifier(ValidationModule.VALIDATOR_FACTORY_BEAN_NAME)
  private LocalValidatorFactoryBean validationFactory;

  /**
   * Publish a {@link Morphia} POJOs mapper for Mongo datatabases.
   *
   * @param configurer The persistent class provider. Required.
   * @return A {@link Morphia} POJOs mapper for Mongo datatabases.
   */
  @Bean
  public Morphia morphia(final MorphiaConfigurer configurer) {
    Validate.notNull(configurer, "The morphia configurer is required.");
    Morphia morphia = new Morphia();
    for (Class<?> document : configurer.getClasses()) {
      logger.debug("Adding morphia class: {}", document.getName());
      morphia.map(document);
    }

    if (validationFactory != null) {
      morphia.getMapper().addInterceptor(
          new Jsr303Interceptor(validationFactory));
    }
    return morphia;
  }

  /**
   * Publish a Morphia {@link Datastore} for executing CRUD operations over
   * POJOs.
   *
   * @param morphia The morphia mapper. Required.
   * @param mongo The mongo database connection. Required.
   * @param uri The mongo database uri. Required.
   * @return A Morphia {@link Datastore} for executing CRUD operations over
   *         POJOs.
   */
  @Bean
  public Datastore morphiaDatastore(final Morphia morphia, final Mongo mongo,
      final MongoURI uri) {
    Validate.notNull(morphia, "The morphia mapper is required.");
    Validate.notNull(mongo, "The mongo database connection is required.");
    Validate.notNull(uri, "The mongo database connection uri is required.");
    return morphia.createDatastore(mongo, uri.getDatabase());
  }
}
