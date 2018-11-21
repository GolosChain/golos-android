package io.golos.golos.repository.services.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.golos.golos.repository.model.AppSettings
import io.golos.golos.repository.model.NotificationSettings

/**
 * Created by yuri yurivladdurain@gmail.com on 21/11/2018.
 */
@JsonSerialize(using = GolosSettingLanguageSerializer::class)
@JsonDeserialize(using = GolosSettingsDeserializer::class)
enum class GolosServiceSettingsLanguage {
    ENGLISH, RUSSIAN
}

class GolosSettingLanguageSerializer : JsonSerializer<GolosServiceSettingsLanguage>() {
    override fun serialize(value: GolosServiceSettingsLanguage?, gen: JsonGenerator?, serializers: SerializerProvider?) {
        gen?.writeString(if (value == GolosServiceSettingsLanguage.RUSSIAN) "ru" else "en")
    }
}

class GolosSettingsDeserializer : JsonDeserializer<GolosServiceSettingsLanguage>() {

    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): GolosServiceSettingsLanguage {
        p ?: return GolosServiceSettingsLanguage.ENGLISH
        val codec = p.codec
        val rootNode: TreeNode = codec?.readTree(p) ?: return GolosServiceSettingsLanguage.ENGLISH
        return try {
            if (rootNode.toString() == "ru") GolosServiceSettingsLanguage.RUSSIAN else GolosServiceSettingsLanguage.ENGLISH
        } catch (e: Exception) {
            GolosServiceSettingsLanguage.ENGLISH
        }
    }
}

class GolosServicesSettings(
        @JsonProperty("basic")
        val basic: AppSettings?,
        @JsonProperty("notify")
        val notify: Any?,
        @JsonProperty("push")
        val push: GolosServicePushSettings?)

class GolosSettingChangeRequest(
        @JsonProperty("profile")//device id
        val profile: String,
        @JsonProperty("basic")
        val basic: AppSettings?,
        @JsonProperty("notify")
        val notify: Any?,
        @JsonProperty("push")
        val push: GolosServicePushSettings?
) : GolosServicesRequest()

class GolosServicesSettingsRequest(
        @JsonProperty("profile")//device id
        val profile: String) : GolosServicesRequest()

class GolosServicePushSettings(
        @JsonProperty("lang")
        val lang: GolosServiceSettingsLanguage,
        @JsonProperty("show")
        val show: NotificationSettings?)