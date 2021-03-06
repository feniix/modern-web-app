package org.knowhow.mwa.validation;

import org.knowhow.mwa.handler.BindHandlerExceptionResolver;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.MethodArgumentNotValidException;

/**
 * Handle {@link MethodArgumentNotValidException} and print them using a
 * {@link HttpMessageConverter}.
 *
 * @author edgar.espina
 * @since 0.1
 */
public class Jsr303HandlerExceptionResolver extends
    BindHandlerExceptionResolver {

  /**
   * Creates a new {@link Jsr303HandlerExceptionResolver}.
   */
  public Jsr303HandlerExceptionResolver() {
    super(MethodArgumentNotValidException.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected Object convert(final Exception exception) {
    return super.convert(((MethodArgumentNotValidException) exception)
        .getBindingResult());
  }

}
