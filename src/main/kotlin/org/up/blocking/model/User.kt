package org.up.blocking.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.*
import kotlin.jvm.Transient

@Entity(name = "users")
data class UserJpa(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,
    @Column(name = "user_name")
    val userName: String,
    val email: String,
    @Column(name = "email_verified")
    val emailVerified: Boolean,
    @Column(name = "avatar_url")
    val avatarUrl: String?,
) {
    fun toDto() = UserDto(userName, email, emailVerified, avatarUrl)
}


data class UserDto(
    val userName: String,
    val email: String,
    val emailVerified: Boolean,
    val avatarUrl: String?,

    val threadName: String = Thread.currentThread().isVirtual.let {
        val name = """ ${Thread.currentThread().threadGroup.name}/${Thread.currentThread().name}"""
        if (it) "VirtualThread-$name" else name
    },
)

data class AvatarDto
@JsonCreator
constructor(
    @JsonProperty("url") val url: String,
)