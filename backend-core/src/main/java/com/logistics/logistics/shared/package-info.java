/**
 * Núcleo compartilhado: tipos comuns (coordenadas, paginação, exceções base), segurança, tratamento de erros
 * HTTP e configuração. Aberto: qualquer módulo usa qualquer subpacote. Não depende de nenhum módulo; o que
 * for de um contexto só fica no módulo dele.
 */
@ApplicationModule(displayName = "Compartilhado", type = ApplicationModule.Type.OPEN)
package com.logistics.logistics.shared;

import org.springframework.modulith.ApplicationModule;
