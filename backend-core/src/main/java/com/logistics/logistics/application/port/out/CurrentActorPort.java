package com.logistics.logistics.application.port.out;

/** Identifica quem executa o caso de uso (usuário autenticado ou "system"). */
public interface CurrentActorPort {
  String currentActor();
}
