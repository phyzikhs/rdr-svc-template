package com.radioretail.template.handles

import com.fullfacing.keycloak4s.auth.core.PolicyBuilders
import com.fullfacing.keycloak4s.auth.core.authorization.PathAuthorization
import com.fullfacing.keycloak4s.auth.core.validation.TokenValidator
import com.fullfacing.keycloak4s.core.models.{ConfigWithoutAuth, KeycloakConfig}

object Keycloak {

  val config: KeycloakConfig = ConfigWithoutAuth(
    scheme = sys.env("KEYCLOAK_SCHEME"),
    host   = sys.env("KEYCLOAK_HOST"),
    port   = sys.env("KEYCLOAK_PORT").toInt,
    realm  = sys.env("KEYCLOAK_REALM")
  )

  implicit val validator: TokenValidator = TokenValidator.Dynamic(config)
  implicit val auth: PathAuthorization = PolicyBuilders.buildPathAuthorization("keycloak.json")
}
