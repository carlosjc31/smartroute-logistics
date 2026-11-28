package com.logistics.logistics.shared.application;

/**
 * Um serviço externo do qual a operação depende não respondeu. Vira 503 com {@link #userMessage()}.
 * A mensagem técnica e a causa não vão para a resposta, e o log registra só o tipo da causa: a mensagem do
 * cliente HTTP pode trazer a URL com dados pessoais (por exemplo a busca de geocodificação, LGPD).
 */
public abstract class DependencyUnavailableException extends RuntimeException {

  private final String dependency;
  private final String userMessage;

  protected DependencyUnavailableException(String dependency, String userMessage, String message, Throwable cause) {
    super(message, cause);
    this.dependency = dependency;
    this.userMessage = userMessage;
  }

  /** Nome da dependência para o log, por exemplo "Geocodificação". */
  public String dependency() {
    return dependency;
  }

  public String userMessage() {
    return userMessage;
  }
}
