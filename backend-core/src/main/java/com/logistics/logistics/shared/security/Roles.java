package com.logistics.logistics.shared.security;

import java.util.Set;

/** Perfis do realm {@code smartroute} no Keycloak. */
public final class Roles {
  public static final String ADMIN = "ADMIN";
  public static final String DISPATCHER = "DISPATCHER";
  public static final String DRIVER = "DRIVER";
  public static final String CUSTOMER = "CUSTOMER";

  static final Set<String> ALL = Set.of(ADMIN, DISPATCHER, DRIVER, CUSTOMER);

  private Roles() {}
}
