package com.todoker.todokerbackend.repository

import com.todoker.todokerbackend.domain.user.User
import com.todoker.todokerbackend.domain.user.UserPreference
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserPreferenceRepository : JpaRepository<UserPreference, Long> {
    
    fun findByUser(user: User): Optional<UserPreference>
    
    fun findByUserId(userId: Long): Optional<UserPreference>
    
    fun existsByUser(user: User): Boolean
}