package com.jsh.pos.infrastructure.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.jsh.pos.application.port.out.NoteListCachePort
import com.jsh.pos.domain.note.Note
import com.jsh.pos.infrastructure.cache.NoteListCacheProperties
import com.jsh.pos.infrastructure.cache.RedisNoteListCacheAdapter
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate

@Configuration
@EnableConfigurationProperties(NoteListCacheProperties::class)
class NoteListCacheConfig {

    @Bean
    fun noteListCachePort(
        redisConnectionFactoryProvider: ObjectProvider<RedisConnectionFactory>,
        objectMapper: ObjectMapper,
        properties: NoteListCacheProperties,
    ): NoteListCachePort {
        val redisConnectionFactory = redisConnectionFactoryProvider.getIfAvailable()
        if (redisConnectionFactory == null) {
            logger.info("[note-list-cache] no-op cache port enabled (Redis not connected)")
            return object : NoteListCachePort {
                override fun getOrLoad(query: NoteListCachePort.Query, loader: () -> List<Note>): List<Note> = loader()

                override fun evictOwner(ownerUsername: String) = Unit
            }
        }

        val stringRedisTemplate = StringRedisTemplate(redisConnectionFactory)
        stringRedisTemplate.afterPropertiesSet()
        logger.info("[note-list-cache] RedisNoteListCacheAdapter enabled")
        return RedisNoteListCacheAdapter(stringRedisTemplate, objectMapper, properties)
    }


    private companion object {
        private val logger = LoggerFactory.getLogger(NoteListCacheConfig::class.java)
    }
}

