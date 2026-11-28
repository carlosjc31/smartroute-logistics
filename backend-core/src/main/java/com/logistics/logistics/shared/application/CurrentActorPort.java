package com.logistics.logistics.shared.application;

/** Identifica quem executa o caso de uso (usuário autenticado ou "system"). */
public interface CurrentActorPort {
  String currentActor();
}
