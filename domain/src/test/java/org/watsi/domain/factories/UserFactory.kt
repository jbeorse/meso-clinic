package org.watsi.domain.factories

import org.watsi.domain.entities.User

object UserFactory {

    fun build(
            id: Int = 1,
            username: String = "foo",
            name: String = "Foo",
            providerId: Int = 1,
            providerType: User.ProviderType = User.ProviderType.HEALTH_CENTER,
            role: String = "provider_admin",
            securityPin: String = "0000") : User {
        return User(
            id = id,
            username = username,
            name = name,
            providerId = providerId,
            providerType = providerType,
            role = role,
            securityPin = securityPin
        )
    }
}
